/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.salesforce.grpc.contrib.AmbientContext;
import io.grpc.*;

public class AmbientContextInterceptor {
    public static class Client implements ClientInterceptor {
        private String headerPrefix;

        public Client(String headerPrefix) {
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
                    Metadata ctx = AmbientContext.get();
                    if (ctx != null) {
                        for (String keyString : ctx.keys()) {
                            if (keyString.startsWith(headerPrefix)) {
                                Metadata.Key<String> key = Metadata.Key.of(keyString, Metadata.ASCII_STRING_MARSHALLER);
                                String value = ctx.get(key);
                                if (value != null) {
                                    headers.put(key, value);
                                }
                            }
                        }
                    }
                    super.start(responseListener, headers);
                }
            };
        }
    }

    public static class Server implements ServerInterceptor {
        private String headerPrefix;

        public Server(String headerPrefix) {
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

            final ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
            return new ServerCall.Listener<ReqT>() {
                @Override
                public void onMessage(ReqT message) {
                    Context.current().withValue(AmbientContext.KEY, ctx).run(() -> listener.onMessage(message));
                }

                @Override
                public void onHalfClose() {
                    Context.current().withValue(AmbientContext.KEY, ctx).run(listener::onHalfClose);
                }

                @Override
                public void onCancel() {
                    Context.current().withValue(AmbientContext.KEY, ctx).run(listener::onCancel);
                }

                @Override
                public void onComplete() {
                    Context.current().withValue(AmbientContext.KEY, ctx).run(listener::onComplete);
                }

                @Override
                public void onReady() {
                    Context.current().withValue(AmbientContext.KEY, ctx).run(listener::onReady);
                }
            };
        }
    }
}
