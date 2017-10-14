/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.*;

/**
 * {@code AmbientContextFreezeServerInterceptor} freezes the current {@link AmbientContext} for all downstream
 * operations. The best place to put this interceptor is at the end of the gRPC interceptor chain.
 */
public class AmbientContextFreezeServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(call, headers)) {
            @Override
            public void onMessage(ReqT message) {
                freezeAndThaw(() -> super.onMessage(message));
            }

            @Override
            public void onHalfClose() {
                freezeAndThaw(super::onHalfClose);
            }

            @Override
            public void onCancel() {
                freezeAndThaw(super::onCancel);
            }

            @Override
            public void onComplete() {
                freezeAndThaw(super::onComplete);
            }

            @Override
            public void onReady() {
                freezeAndThaw(super::onReady);
            }

            private void freezeAndThaw(Runnable delegate) {
                if (AmbientContext.isPresent()) {
                    Object freezeKey = AmbientContext.current().freeze();
                    try {
                        delegate.run();
                    } finally {
                        AmbientContext.current().thaw(freezeKey);
                    }
                } else {

                    delegate.run();
                }
            }
        };
    }
}
