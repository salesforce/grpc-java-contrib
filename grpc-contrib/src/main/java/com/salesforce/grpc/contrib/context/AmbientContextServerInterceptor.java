/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.*;

public class AmbientContextServerInterceptor implements ServerInterceptor {
    private String headerPrefix;

    public AmbientContextServerInterceptor(String headerPrefix) {
        this.headerPrefix = headerPrefix;
    }

    public String getHeaderPrefix() {
        return headerPrefix;
    }

    public void setHeaderPrefix(String headerPrefix) {
        this.headerPrefix = headerPrefix;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        final Metadata ctx = new Metadata();
        for (String keyName : headers.keys()) {
            if (!keyName.startsWith(headerPrefix)) {
                continue;
            }

            Metadata.Key<String> key = Metadata.Key.of(keyName, Metadata.ASCII_STRING_MARSHALLER);
            String value = headers.get(key);
            if (value == null) {
                continue;
            }

            ctx.put(key, value);
        }

        return Contexts.interceptCall(Context.current().withValue(AmbientContext.KEY, ctx), call, headers, next);
    }
}