/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.*;

/**
 * {@code AmbientContextServerInterceptor} transparently serializes prefixed ambient context values into outbound request
 * headers.
 *
 * <p>Each {@code AmbientContextServerInterceptor} marshals headers with a know prefix. If multiple prefixes are needed,
 * add multiple {@code AmbientContextServerInterceptor} instances to the gRPC interceptor chain.
 *
 * <p>See package javadoc for more info.
 */
public class AmbientContextServerInterceptor implements ServerInterceptor {
    private String headerPrefix;

    /**
     * Constructs an {@code AmbientContextServerInterceptor} that marshals ambient context values with a know prefix
     * into outbound request headers.
     * {@link AmbientContext}.
     *
     * @param headerPrefix the header prefix to marshal.
     */
    public AmbientContextServerInterceptor(String headerPrefix) {
        this.headerPrefix = headerPrefix;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        AmbientContext ctx = AmbientContext.DATA_KEY.get();
        // Only initialize ctx if not yet initialized
        ctx = ctx != null ? ctx : new AmbientContext();

        boolean found = false;
        for (String keyName : headers.keys()) {
            if (!keyName.startsWith(headerPrefix)) {
                continue;
            }

            if (keyName.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                Metadata.Key<byte[]> key = Metadata.Key.of(keyName, Metadata.BINARY_BYTE_MARSHALLER);
                Iterable<byte[]> values = headers.getAll(key);
                if (values == null) {
                    continue;
                }

                for (byte[] value : values) {
                    ctx.put(key, value);
                }
            } else {
                Metadata.Key<String> key = Metadata.Key.of(keyName, Metadata.ASCII_STRING_MARSHALLER);
                Iterable<String> values = headers.getAll(key);
                if (values == null) {
                    continue;
                }

                for (String value : values) {
                    ctx.put(key, value);
                }
            }

            found = true;
        }

        if (found) {
            return Contexts.interceptCall(Context.current().withValue(AmbientContext.DATA_KEY, ctx), call, headers, next);
        } else {
            // Don't attach a context if there is nothing to attach
            return next.startCall(call, headers);
        }
    }
}