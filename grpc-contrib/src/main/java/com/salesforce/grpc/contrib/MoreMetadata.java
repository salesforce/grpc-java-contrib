/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Metadata;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@code MoreMetadata} provides additional utilities for working with gRPC {@code Metadata}.
 */
//CHECKSTYLE:OFF: MethodName
public final class MoreMetadata {
    private MoreMetadata() { }

    /**
     * A metadata marshaller that encodes objects as JSON using the google-gson library.
     *
     * <p>All non-ascii characters are unicode escaped to comply with {@code AsciiMarshaller}'s character range
     * requirements.
     *
     * @param clazz the type to serialize
     * @param <T>
     */
    public static final <T> Metadata.AsciiMarshaller<T> JSON_MARSHALLER(Class<T> clazz) {
        return new Metadata.AsciiMarshaller<T>() {
            TypeToken<T> typeToken = TypeToken.of(clazz);
            private Gson gson = new Gson();

            @Override
            public String toAsciiString(T value) {
                try {
                    try (StringWriter sw = new StringWriter()) {
                        gson.toJson(value, typeToken.getType(), new UnicodeEscapingAsciiWriter(sw));
                        return sw.toString();
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            @Override
            public T parseAsciiString(String serialized) {
                return gson.fromJson(serialized, typeToken.getType());
            }
        };
    }

    /**
     * See: https://github.com/google/gson/issues/388.
     */
    private static final class UnicodeEscapingAsciiWriter extends Writer {
        private final Writer out;

        private UnicodeEscapingAsciiWriter(Writer out) {
            this.out = out;
        }

        @Override public void write(char[] buffer, int offset, int count) throws IOException {
            for (int i = 0; i < count; i++) {
                char c = buffer[i + offset];
                if (c >= ' ' && c <= '~') {
                    out.write(c);
                } else {
                    out.write(String.format("\\u%04x", (int) c));
                }
            }
        }

        @Override public void flush() throws IOException {
            out.flush();
        }

        @Override public void close() throws IOException {
            out.close();
        }
    }

    /**
     * A metadata marshaller that encodes objects as protobuf according to their proto IDL specification.
     *
     * @param clazz the type to serialize
     * @param <T>
     */
    public static <T extends GeneratedMessageV3> Metadata.BinaryMarshaller<T> PROTOBUF_MARSHALLER(Class<T> clazz) {
        try {
            Method defaultInstance = clazz.getMethod("getDefaultInstance");
            GeneratedMessageV3 instance = (GeneratedMessageV3) defaultInstance.invoke(null);

            return new Metadata.BinaryMarshaller<T>() {
                @Override
                public byte[] toBytes(T value) {
                    return value.toByteArray();
                }

                @Override
                public T parseBytes(byte[] serialized) {
                    try {
                        return (T) instance.getParserForType().parseFrom(serialized);
                    } catch (InvalidProtocolBufferException ipbe) {
                        throw new IllegalArgumentException(ipbe);
                    }
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * A metadata marshaller that encodes boolean values.
     */
    public static final Metadata.AsciiMarshaller<Boolean> BOOLEAN_MARSHALLER = new Metadata.AsciiMarshaller<Boolean>() {
        @Override
        public String toAsciiString(Boolean value) {
            return value.toString();
        }

        @Override
        public Boolean parseAsciiString(String serialized) {
            return Boolean.parseBoolean(serialized);
        }
    };

    /**
     * A metadata marshaller that encodes integer-type values.
     */
    public static final Metadata.AsciiMarshaller<Long> LONG_MARSHALLER = new Metadata.AsciiMarshaller<Long>() {
        @Override
        public String toAsciiString(Long value) {
            return value.toString();
        }

        @Override
        public Long parseAsciiString(String serialized) {
            return Long.parseLong(serialized);
        }
    };

    /**
     * A metadata marshaller that encodes floating-point-type values.
     */
    public static final Metadata.AsciiMarshaller<Double> DOUBLE_MARSHALLER = new Metadata.AsciiMarshaller<Double>() {
        @Override
        public String toAsciiString(Double value) {
            return value.toString();
        }

        @Override
        public Double parseAsciiString(String serialized) {
            return Double.parseDouble(serialized);
        }
    };
}
