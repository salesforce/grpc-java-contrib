package com.salesforce.jprotoc;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos;

import java.util.Collection;

import javax.annotation.Nonnull;

/**
 * {@code ProtoTypeMap} maintains a dictionary for looking up Java type names when given proto types.
 */
class ProtoTypeMap {

    private final ImmutableMap<String, String> types;

    private ProtoTypeMap(@Nonnull ImmutableMap<String, String> types) {
        Preconditions.checkNotNull(types, "types");

        this.types = types;
    }

    /**
     * Returns an instance of {@link ProtoTypeMap} based on the given {@link DescriptorProtos.FileDescriptorProto}
     * instances.
     *
     * @param fileDescriptorProtos the full collection of files descriptors from the code generator request
     */
    public static ProtoTypeMap of(Collection<DescriptorProtos.FileDescriptorProto> fileDescriptorProtos) {
        final ImmutableMap.Builder<String, String> types = ImmutableMap.builder();

        for (final DescriptorProtos.FileDescriptorProto fileDescriptor : fileDescriptorProtos) {
            final DescriptorProtos.FileOptions fileOptions = fileDescriptor.getOptions();

            final String protoPackage = fileDescriptor.hasPackage() ? "." + fileDescriptor.getPackage() : "";
            final String javaPackage = Strings.emptyToNull(
                    fileOptions.hasJavaPackage()
                    ? fileOptions.getJavaPackage()
                    : fileDescriptor.getPackage());
            final String enclosingClassName =
                    fileOptions.getJavaMultipleFiles()
                    ? null
                    : getJavaOuterClassname(fileDescriptor, fileOptions);

            fileDescriptor.getEnumTypeList().forEach(
                    e -> types.put(
                            protoPackage + "." + e.getName(),
                            ProtoTypeUtils.toJavaTypeName(e.getName(), enclosingClassName, javaPackage)));

            fileDescriptor.getMessageTypeList().forEach(
                    m -> types.put(
                            protoPackage + "." + m.getName(),
                            ProtoTypeUtils.toJavaTypeName(m.getName(), enclosingClassName, javaPackage)));
        }

        return new ProtoTypeMap(types.build());
    }

    /**
     * Returns the full Java type name for the given proto type.
     *
     * @param protoTypeName the proto type to be converted to a Java type
     */
    String toJavaTypeName(String protoTypeName) {
        return types.get(protoTypeName);
    }

    private static String getJavaOuterClassname(
            DescriptorProtos.FileDescriptorProto fileDescriptor,
            DescriptorProtos.FileOptions fileOptions) {

        if (fileOptions.hasJavaOuterClassname()) {
            return fileOptions.getJavaOuterClassname();
        }

        // If the outer class name is not explicitly defined, then we take the proto filename, strip its extension,
        // and convert it from snake case to camel case.
        String filename = fileDescriptor.getName().substring(0, fileDescriptor.getName().length() - ".proto".length());

        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, filename);
    }
}
