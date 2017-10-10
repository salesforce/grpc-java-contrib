/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.*;

/**
 * {@code AmbientContextClientInterceptor} transparently deserializes prefixed request headers into an ambient context.
 * Header values can be accessed using the {@link AmbientContext} class.
 *
 * <p>Each {@code AmbientContextClientInterceptor} marshals headers with a know prefix. If multiple prefixes are needed,
 * add multiple {@code AmbientContextClientInterceptor} instances to the gRPC interceptor chain.
 *
 * <p>See package javadoc for more info.
 */
public class AmbientContextClientInterceptor implements ClientInterceptor {
    private String headerPrefix;

    /**
     * Constructs an {@code AmbientContextClientInterceptor} that marshals request headers with a know prefix into the
     * {@link AmbientContext}.
     *
     * @param headerPrefix the header prefix to marshal.
     */
    public AmbientContextClientInterceptor(String headerPrefix) {
        this.headerPrefix = headerPrefix;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        if (AmbientContext.isPresent()) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    AmbientContext ctx = AmbientContext.current();
                    if (ctx != null) {
                        for (String keyString : ctx.keys()) {
                            if (keyString.startsWith(headerPrefix)) {
                                if (keyString.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                                    Metadata.Key<byte[]> key = Metadata.Key.of(keyString, Metadata.BINARY_BYTE_MARSHALLER);
                                    Iterable<byte[]> values = ctx.getAll(key);
                                    if (values != null) {
                                        for (byte[] value : values) {
                                            headers.put(key, value);
                                        }
                                    }
                                } else {
                                    Metadata.Key<String> key = Metadata.Key.of(keyString, Metadata.ASCII_STRING_MARSHALLER);
                                    Iterable<String> values = ctx.getAll(key);
                                    if (values != null) {
                                        for (String value : values) {
                                            headers.put(key, value);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    super.start(responseListener, headers);
                }
            };
        } else {
            // Noop if ambient context is absent
            return next.newCall(method, callOptions);
        }
    }
}
