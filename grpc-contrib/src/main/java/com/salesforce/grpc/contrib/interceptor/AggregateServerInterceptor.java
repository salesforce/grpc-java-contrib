/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.google.common.collect.Lists;
import io.grpc.*;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * AggregateClientInterceptor is used to bundle multiple {@link ServerInterceptor} implementations into a single
 * {@code ServerInterceptor}. Each inner {@code ServerInterceptor} is applied in order when
 * {@link AggregateServerInterceptor#interceptCall(ServerCall, Metadata, ServerCallHandler)} is called.
 */

public class AggregateServerInterceptor implements ServerInterceptor {
    private final List<ServerInterceptor> interceptors;

    /**
     * Construct a AggregateServerInterceptor from one or more {@link ServerInterceptor}s. The inner
     * {@code ClientInterceptor}s will be called in order.
     *
     * @param interceptors a {@link ServerInterceptor} array
     */
    public AggregateServerInterceptor(ServerInterceptor... interceptors) {
        this(Arrays.asList(checkNotNull(interceptors, "interceptors")));
    }

    /**
     * Construct a AggregateServerInterceptor from one or more {@link ServerInterceptor}s. The inner
     * {@code ClientInterceptor}s will be called in order.
     *
     * @param interceptors a {@link ServerInterceptor} list
     */
    public AggregateServerInterceptor(List<ServerInterceptor> interceptors) {
        checkNotNull(interceptors, "interceptors");
        checkArgument(interceptors.size() > 0,
                "AggregateServerInterceptor requires at least one inner ServerInterceptor.");
        this.interceptors = interceptors;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        // reverse the interceptors list so that the last interceptor to call is the most nested interceptor
        for (ServerInterceptor interceptor : Lists.reverse(interceptors)) {
            next = new InterceptorServerCallHandler<>(next, interceptor);
        }

        return next.startCall(call, headers);
    }

    /**
     * A {@link ServerCallHandler} implementation used to chain {@link ServerInterceptor} instances together.
     * @param <ReqT>
     * @param <RespT>
     */
    private static class InterceptorServerCallHandler<ReqT, RespT> implements ServerCallHandler<ReqT, RespT> {
        private final ServerCallHandler<ReqT, RespT> next;
        private final ServerInterceptor interceptor;

        private InterceptorServerCallHandler(ServerCallHandler<ReqT, RespT> next, ServerInterceptor interceptor) {
            this.next = next;
            this.interceptor = interceptor;
        }

        @Override
        public ServerCall.Listener<ReqT> startCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            return interceptor.interceptCall(serverCall, metadata, next);
        }
    }
}
