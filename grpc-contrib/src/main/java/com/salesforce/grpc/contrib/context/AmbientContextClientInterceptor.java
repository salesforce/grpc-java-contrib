/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.*;

public class AmbientContextClientInterceptor implements ClientInterceptor {
    private String headerPrefix;

    public AmbientContextClientInterceptor(String headerPrefix) {
        this.headerPrefix = headerPrefix;
    }

    public String getHeaderPrefix() {
        return headerPrefix;
    }

    public void setHeaderPrefix(String headerPrefix) {
        this.headerPrefix = headerPrefix;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                Metadata ctx = AmbientContext.current();
                if (ctx != null) {
                    for (String keyString : ctx.keys()) {
                        if (keyString.startsWith(headerPrefix)) {
                            if (keyString.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                                Metadata.Key<byte[]> key = Metadata.Key.of(keyString, Metadata.BINARY_BYTE_MARSHALLER);
                                byte[] value = ctx.get(key);
                                if (value != null) {
                                    headers.put(key, value);
                                }
                            } else {
                                Metadata.Key<String> key = Metadata.Key.of(keyString, Metadata.ASCII_STRING_MARSHALLER);
                                String value = ctx.get(key);
                                if (value != null) {
                                    headers.put(key, value);
                                }
                            }
                        }
                    }
                }
                super.start(responseListener, headers);
            }
        };
    }
}
