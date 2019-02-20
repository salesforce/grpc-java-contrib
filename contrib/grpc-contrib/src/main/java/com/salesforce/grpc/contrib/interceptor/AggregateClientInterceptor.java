/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
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
 * AggregateClientInterceptor is used to bundle multiple {@link ClientInterceptor} implementations into a single
 * {@code ClientInterceptor}. Each inner {@code ClientInterceptor} is applied in order when
 * {@link AggregateClientInterceptor#interceptCall(MethodDescriptor, CallOptions, Channel)} is called.
 */
public class AggregateClientInterceptor implements ClientInterceptor {
    private final List<ClientInterceptor> interceptors;

    /**
     * Construct a AggregateClientInterceptor from one or more {@link ClientInterceptor}s. The inner
     * {@code ClientInterceptor}s will be called in order.
     *
     * @param interceptors a {@link ClientInterceptor} array
     */
    public AggregateClientInterceptor(ClientInterceptor... interceptors) {
        this(Arrays.asList(checkNotNull(interceptors, "interceptors")));
    }

    /**
     * Construct a AggregateClientInterceptor from one or more {@link ClientInterceptor}s. The inner
     * {@code ClientInterceptor}s will be called in order.
     *
     * @param interceptors a {@link ClientInterceptor} list
     */
    public AggregateClientInterceptor(List<ClientInterceptor> interceptors) {
        checkNotNull(interceptors, "interceptors");
        checkArgument(interceptors.size() > 0,
                "AggregateClientInterceptor requires at least one inner ClientInterceptor.");
        this.interceptors = interceptors;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        // reverse the interceptors list so that the last interceptor to call is the most nested interceptor
        for (ClientInterceptor interceptor : Lists.reverse(interceptors)) {
            next = new InterceptorChannel(next, interceptor);
        }

        return next.newCall(method, callOptions);
    }

    /**
     * A {@link Channel} implementation used to chain {@link ClientInterceptor} instances together.
     */
    private static final class InterceptorChannel extends Channel {
        private final Channel channel;
        private final ClientInterceptor interceptor;

        private InterceptorChannel(Channel channel, ClientInterceptor interceptor) {
            this.channel = channel;
            this.interceptor = checkNotNull(interceptor, "interceptor");
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions) {
            return interceptor.interceptCall(method, callOptions, channel);
        }

        @Override
        public String authority() {
            return channel.authority();
        }
    }
}
