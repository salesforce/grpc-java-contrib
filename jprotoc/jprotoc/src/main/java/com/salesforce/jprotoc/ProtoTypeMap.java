/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.jprotoc;

import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos;

import java.util.Collection;

import javax.annotation.Nonnull;

/**
 * {@code ProtoTypeMap} maintains a dictionary for looking up Java type names when given proto types.
 */
public final class ProtoTypeMap {

    private static final Joiner DOT_JOINER = Joiner.on('.').skipNulls();
    private final ImmutableMap<String, String> types;

    private ProtoTypeMap(@Nonnull ImmutableMap<String, String> types) {
        Preconditions.checkNotNull(types, "types");

        this.types = types;
    }

    /**
     * Returns an instance of {@link ProtoTypeMap} based on the given FileDescriptorProto instances.
     *
     * @param fileDescriptorProtos the full collection of files descriptors from the code generator request
     */
    public static ProtoTypeMap of(@Nonnull Collection<DescriptorProtos.FileDescriptorProto> fileDescriptorProtos) {
        Preconditions.checkNotNull(fileDescriptorProtos, "fileDescriptorProtos");
        Preconditions.checkArgument(!fileDescriptorProtos.isEmpty(), "fileDescriptorProtos.isEmpty()");

        final ImmutableMap.Builder<String, String> types = ImmutableMap.builder();

        for (final DescriptorProtos.FileDescriptorProto fileDescriptor : fileDescriptorProtos) {
            final DescriptorProtos.FileOptions fileOptions = fileDescriptor.getOptions();

            final String protoPackage = fileDescriptor.hasPackage() ? "." + fileDescriptor.getPackage() : "";
            final String javaPackage = Strings.emptyToNull(fileOptions.hasJavaPackage() ? fileOptions.getJavaPackage() : fileDescriptor.getPackage());
            final String enclosingClassName = fileOptions.getJavaMultipleFiles() ? null : getJavaOuterClassname(fileDescriptor);

            // Identify top-level enums
            fileDescriptor.getEnumTypeList().forEach(e -> types.put(
                    protoPackage + "." + e.getName(),
                    DOT_JOINER.join(javaPackage, enclosingClassName, e.getName())));

            // Identify top-level messages, and nested types
            fileDescriptor.getMessageTypeList().forEach(
                m -> recursivelyAddTypes(types, m, protoPackage, enclosingClassName, javaPackage)
            );
        }

        return new ProtoTypeMap(types.build());
    }

    private static void recursivelyAddTypes(ImmutableMap.Builder<String, String> types, DescriptorProtos.DescriptorProto m, String protoPackage, String enclosingClassName, String javaPackage) {
        // Identify current type
        String protoTypeName = protoPackage + "." + m.getName();
        types.put(
            protoTypeName,
            DOT_JOINER.join(javaPackage, enclosingClassName, m.getName()));

        // Identify any nested Enums
        m.getEnumTypeList().forEach(
            e -> types.put(
                protoPackage + "." + m.getName() + "." + e.getName(),
                DOT_JOINER.join(javaPackage, enclosingClassName, m.getName(), e.getName())));

        // Recursively identify any nested types
        m.getNestedTypeList().forEach(
            n -> recursivelyAddTypes(
                types,
                n,
                protoPackage + "." + m.getName(),
                DOT_JOINER.join(enclosingClassName, m.getName()),
                javaPackage));
    }

    /**
     * Returns the full Java type name for the given proto type.
     *
     * @param protoTypeName the proto type to be converted to a Java type
     */
    public String toJavaTypeName(@Nonnull String protoTypeName) {
        Preconditions.checkNotNull(protoTypeName, "protoTypeName");
        return types.get(protoTypeName);
    }

    /**
     * Computes the Java outer class name for a given FileDescriptorProto, for use when multiple classes are generated
     * in the same outer class.
     * @param fileDescriptor
     * @return
     */
    public static String getJavaOuterClassname(DescriptorProtos.FileDescriptorProto fileDescriptor) {
        Preconditions.checkNotNull(fileDescriptor, "fileDescriptor");
        DescriptorProtos.FileOptions fileOptions = fileDescriptor.getOptions();

        if (fileOptions.hasJavaOuterClassname()) {
            return fileOptions.getJavaOuterClassname();
        }

        // If the outer class name is not explicitly defined, then we take the proto filename, strip its extension,
        // and convert it from snake case to camel case.
        String filename = fileDescriptor.getName().substring(0, fileDescriptor.getName().length() - ".proto".length());

        // Protos in subdirectories without java_outer_classname have their path prepended to the filename. Remove
        // if present.
        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf('/') + 1);
        }

        filename = makeInvalidCharactersUnderscores(filename);
        filename = convertToCamelCase(filename);
        filename = appendOuterClassSuffix(filename, fileDescriptor);
        return filename;
    }

    /**
     * In the event of a name conflict between the outer and inner type names, protoc adds an OuterClass suffix to the
     * outer type's name.
     */
    private static String appendOuterClassSuffix(final String enclosingClassName, DescriptorProtos.FileDescriptorProto fd) {
        if (fd.getEnumTypeList().stream().anyMatch(enumProto -> enumProto.getName().equals(enclosingClassName)) ||
            fd.getMessageTypeList().stream().anyMatch(messageProto -> messageProto.getName().equals(enclosingClassName)) ||
            fd.getServiceList().stream().anyMatch(serviceProto -> serviceProto.getName().equals(enclosingClassName))) {
            return enclosingClassName + "OuterClass";
        } else {
            return enclosingClassName;
        }
    }

    /**
     * Replace invalid proto identifier characters with an underscore, so they will be dropped and camel cases below.
     * https://developers.google.com/protocol-buffers/docs/reference/proto3-spec
     */
    private static String makeInvalidCharactersUnderscores(String filename) {
        char[] filechars = filename.toCharArray();
        for (int i = 0; i < filechars.length; i++) {
            char c = filechars[i];
            if (!CharMatcher.inRange('0', '9').or(CharMatcher.inRange('A', 'Z')).or(CharMatcher.inRange('a', 'z')).matches(c)) {
                filechars[i] = '_';
            }
        }
        return new String(filechars);
    }

    /**
     * Adjust a class name to follow the JavaBean spec.
     * - capitalize the first letter
     * - remove embedded underscores & capitalize the following letter
     * - capitalize letter after a number
     *
     * @param name method name
     * @return lower name
     */
    private static String convertToCamelCase(String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(name.charAt(0)));

        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            char prev = name.charAt(i - 1);

            if (c != '_') {
                if (prev == '_' || CharMatcher.inRange('0', '9').matches(prev)) {
                    sb.append(Character.toUpperCase(c));
                } else {
                    sb.append(c);
                }
            }
        }

        return sb.toString();
    }

}
