package com.salesforce.jprotoc;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * {@code ProtoTypeUtils} contains static helper methods for working with protobuf types.
 */
class ProtoTypeUtils {

    private static final Joiner DOT_JOINER = Joiner.on('.').skipNulls();

    /**
     * Returns the full Java type name based on the given protobuf type parameters.
     *
     * @param className the protobuf type name
     * @param enclosingClassName the optional enclosing class for the given type
     * @param javaPackage the proto file's configured java package name
     */
    static String toJavaTypeName(
            @Nonnull String className,
            @Nullable String enclosingClassName,
            @Nullable String javaPackage) {

        Preconditions.checkNotNull(className, "className");

        return DOT_JOINER.join(javaPackage, enclosingClassName, className);
    }

    private ProtoTypeUtils() { }
}
