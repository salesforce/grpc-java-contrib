/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.jprotoc;

import com.google.common.base.Strings;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generates a set of gRPC stubs that support JDK8 {@link java.util.concurrent.CompletableFuture}.
 */
public class Jdk8Generator extends Generator {
    private static final String CLASS_SUFFIX = "Grpc8";

    @Override
    public Stream<PluginProtos.CodeGeneratorResponse.File> generate(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
        final ProtoTypeMap protoTypeMap = ProtoTypeMap.of(request.getProtoFileList());

        return request.getProtoFileList().stream()
                .filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
                .flatMap(f -> extractContext(protoTypeMap, f))
                .map(this::buildFile);
    }

    private Stream<Context> extractContext(ProtoTypeMap protoTypeMap, DescriptorProtos.FileDescriptorProto proto) {
        return proto.getServiceList().stream()
                .map(s -> extractServiceContext(protoTypeMap, s))
                .map(ctx -> {ctx.packageName = extractPackageName(proto); return ctx;})
                .map(ctx -> {ctx.protoName = proto.getName(); return ctx;});
    }

    private String extractPackageName(DescriptorProtos.FileDescriptorProto proto) {
        DescriptorProtos.FileOptions options = proto.getOptions();
        if (options != null) {
            String javaPackage = options.getJavaPackage();
            if (!Strings.isNullOrEmpty(javaPackage)) {
                return javaPackage;
            }
        }

        return Strings.nullToEmpty(proto.getPackage());
    }

    private Context extractServiceContext(
            ProtoTypeMap protoTypeMap,
            DescriptorProtos.ServiceDescriptorProto serviceProto) {
        Context ctx = new Context();
        ctx.fileName = serviceProto.getName() + CLASS_SUFFIX + ".java";
        ctx.className = serviceProto.getName() + CLASS_SUFFIX;
        ctx.serviceName = serviceProto.getName();
        ctx.deprecated = serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated();

        // Identify methods to generate a CompletableFuture-based client for.
        // Only unary methods are supported.
        serviceProto.getMethodList().stream()
                .filter(method -> !method.getClientStreaming() && !method.getServerStreaming())
                .forEach(method -> {
                    ContextMethod ctxMethod = new ContextMethod();
                    ctxMethod.methodName = lowerCaseFirst(method.getName());
                    ctxMethod.inputType = protoTypeMap.toJavaTypeName(method.getInputType());
                    ctxMethod.outputType = protoTypeMap.toJavaTypeName(method.getOutputType());
                    ctxMethod.deprecated = method.getOptions() != null && method.getOptions().getDeprecated();
                    ctx.methods.add(ctxMethod);
                });
        return ctx;
    }

    private String lowerCaseFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private String absoluteFileName(Context ctx) {
        String dir = ctx.packageName.replace('.', '/');
        if (Strings.isNullOrEmpty(dir)) {
            return ctx.fileName;
        } else {
            return dir + "/" + ctx.fileName;
        }
    }

    private PluginProtos.CodeGeneratorResponse.File buildFile(Context context) {
        String content = applyTemplate("Jdk8Stub.mustache", context);
        return PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(absoluteFileName(context))
                .setContent(content)
                .build();
    }

    private class Context {
        public String fileName;
        public String protoName;
        public String packageName;
        public String className;
        public String serviceName;
        public boolean deprecated;

        public final List<ContextMethod> methods = new ArrayList<>();
    }

    private class ContextMethod {
        public String methodName;
        public String inputType;
        public String outputType;
        public boolean deprecated;
    }
}
