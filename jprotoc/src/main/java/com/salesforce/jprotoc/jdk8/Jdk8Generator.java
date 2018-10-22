/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.jprotoc.jdk8;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;
import com.salesforce.jprotoc.ProtocPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates a set of gRPC stubs that support JDK8 {@link java.util.concurrent.CompletableFuture}.
 */
public class Jdk8Generator extends Generator {
    public static void main(String[] args) {
        ProtocPlugin.generate(new Jdk8Generator());
    }

    private static final String CLASS_SUFFIX = "Grpc8";

    private static final String SERVICE_JAVADOC_PREFIX = "    ";
    private static final String METHOD_JAVADOC_PREFIX  = "        ";

    @Override
    public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
        final ProtoTypeMap protoTypeMap = ProtoTypeMap.of(request.getProtoFileList());
        List<PluginProtos.CodeGeneratorResponse.File> files = new ArrayList<>();

        for (DescriptorProtos.FileDescriptorProto protoFile : request.getProtoFileList()) {
            if (request.getFileToGenerateList().contains(protoFile.getName())) {
                for (ServiceContext ctx : extractContext(protoTypeMap, protoFile)) {
                    files.add(buildFile(ctx));
                }
            }
        }

        return files;
    }

    private List<ServiceContext> extractContext(ProtoTypeMap protoTypeMap, DescriptorProtos.FileDescriptorProto fileProto) {
        List<ServiceContext> serviceContexts = new ArrayList<>();

        List<DescriptorProtos.SourceCodeInfo.Location> locations = fileProto.getSourceCodeInfo().getLocationList();
        locations.stream()
                .filter(location -> location.getPathCount() == 2 && location.getPath(0) == DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER)
                .forEach(location -> {
                    int serviceNumber = location.getPath(1);
                    DescriptorProtos.ServiceDescriptorProto serviceProto = fileProto.getService(serviceNumber);
                    ServiceContext ctx = extractServiceContext(protoTypeMap, serviceProto, locations, serviceNumber);
                    ctx.packageName = extractPackageName(fileProto);
                    ctx.protoName = fileProto.getName();
                    ctx.javaDoc = getJavaDoc(getComments(location), SERVICE_JAVADOC_PREFIX);
                    serviceContexts.add(ctx);
                });

        return serviceContexts;
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

    private static final int METHOD_NUMBER_OF_PATHS = 4;
    private ServiceContext extractServiceContext(
            ProtoTypeMap protoTypeMap,
            DescriptorProtos.ServiceDescriptorProto serviceProto,
            List<DescriptorProtos.SourceCodeInfo.Location> locations,
            int serviceNumber) {
        ServiceContext ctx = new ServiceContext();
        ctx.fileName = serviceProto.getName() + CLASS_SUFFIX + ".java";
        ctx.className = serviceProto.getName() + CLASS_SUFFIX;
        ctx.serviceName = serviceProto.getName();
        ctx.deprecated = serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated();

        locations.stream()
                .filter(location -> location.getPathCount() == METHOD_NUMBER_OF_PATHS &&
                        location.getPath(0) == DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER &&
                        location.getPath(1) == serviceNumber &&
                        location.getPath(2) == DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER)
                .forEach(location -> {
                    int methodNumber = location.getPath(METHOD_NUMBER_OF_PATHS - 1);
                    DescriptorProtos.MethodDescriptorProto methodProto = serviceProto.getMethod(methodNumber);

                    // Identify methods to generate a CompletableFuture-based client for.
                    // Only unary methods are supported.
                    if (!methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
                        MethodContext ctxMethod = new MethodContext();
                        ctxMethod.methodName = lowerCaseFirst(methodProto.getName());
                        ctxMethod.inputType = protoTypeMap.toJavaTypeName(methodProto.getInputType());
                        ctxMethod.outputType = protoTypeMap.toJavaTypeName(methodProto.getOutputType());
                        ctxMethod.deprecated = methodProto.getOptions() != null && methodProto.getOptions().getDeprecated();
                        ctxMethod.javaDoc = getJavaDoc(getComments(location), METHOD_JAVADOC_PREFIX);
                        ctx.methods.add(ctxMethod);
                    }
                });
        return ctx;
    }

    private String lowerCaseFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private String absoluteFileName(ServiceContext ctx) {
        String dir = ctx.packageName.replace('.', '/');
        if (Strings.isNullOrEmpty(dir)) {
            return ctx.fileName;
        } else {
            return dir + "/" + ctx.fileName;
        }
    }

    private PluginProtos.CodeGeneratorResponse.File buildFile(ServiceContext context) {
        String content = applyTemplate("Jdk8Stub.mustache", context);
        return PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(absoluteFileName(context))
                .setContent(content)
                .build();
    }

    private String getComments(DescriptorProtos.SourceCodeInfo.Location location) {
        return location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location.getLeadingComments();
    }

    private String getJavaDoc(String comments, String prefix) {
        if (!comments.isEmpty()) {
            StringBuilder builder = new StringBuilder("/**\n")
                    .append(prefix).append(" * <pre>\n");
            Arrays.stream(HtmlEscapers.htmlEscaper().escape(comments).split("\n"))
                    .forEach(line -> builder.append(prefix).append(" * ").append(line).append("\n"));
            builder
                    .append(prefix).append(" * <pre>\n")
                    .append(prefix).append(" */");
            return builder.toString();
        }
        return null;
    }

    /**
     * Backing class for mustache template.
     */
    private class ServiceContext {
        // CHECKSTYLE DISABLE VisibilityModifier FOR 8 LINES
        public String fileName;
        public String protoName;
        public String packageName;
        public String className;
        public String serviceName;
        public boolean deprecated;
        public String javaDoc;
        public final List<MethodContext> methods = new ArrayList<>();
    }

    /**
     * Backing class for mustache template.
     */
    private class MethodContext {
        // CHECKSTYLE DISABLE VisibilityModifier FOR 5 LINES
        public String methodName;
        public String inputType;
        public String outputType;
        public boolean deprecated;
        public String javaDoc;
    }
}
