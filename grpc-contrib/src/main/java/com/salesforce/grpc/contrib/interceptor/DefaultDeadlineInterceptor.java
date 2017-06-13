/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.google.common.base.Preconditions;
import io.grpc.*;

import java.util.concurrent.TimeUnit;

/**
 * DefaultDeadlineInterceptor is used to apply a default request {@link Deadline} to all client requests when a deadline
 * is otherwise missing. If an existing deadline is found in {@link CallOptions} or the {@link Context} the explicit or
 * implicit deadline will be used instead.
 */
public class DefaultDeadlineInterceptor implements ClientInterceptor {
    private final long duration;
    private final TimeUnit timeUnit;

    public DefaultDeadlineInterceptor(long duration, TimeUnit timeUnit) {
        Preconditions.checkArgument(duration > 0, "duration must be greater than zero");
        Preconditions.checkNotNull(timeUnit, "timeUnit");

        this.duration = duration;
        this.timeUnit = timeUnit;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        if (callOptions.getDeadline() == null && Context.current().getDeadline() == null) {
            callOptions = callOptions.withDeadlineAfter(duration, timeUnit);
        }

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

        };
    }
}
