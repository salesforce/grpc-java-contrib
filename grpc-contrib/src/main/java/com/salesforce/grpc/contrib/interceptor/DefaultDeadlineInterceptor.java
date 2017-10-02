/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.google.common.base.Preconditions;
import io.grpc.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * DefaultDeadlineInterceptor is used to apply a default request {@link Deadline} to all client requests when a deadline
 * is otherwise missing. If an existing deadline is found in {@link CallOptions} or the {@link Context} the explicit or
 * implicit deadline will be used instead.
 */
public class DefaultDeadlineInterceptor implements ClientInterceptor {
    private Duration duration;

    public DefaultDeadlineInterceptor(Duration duration) {
        Preconditions.checkNotNull(duration, "duration");
        Preconditions.checkArgument(!duration.isNegative(), "duration must be greater than zero");

        this.duration = duration;
    }

    /**
     * Get the current default deadline duration.
     *
     * @return the current default deadline duration
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Set a new default deadline duration.
     *
     * @param duration the new default deadline duration
     */
    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        // Only add a deadline if no other deadline has been set.
        if (callOptions.getDeadline() == null && Context.current().getDeadline() == null) {
            callOptions = callOptions.withDeadlineAfter(duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

        };
    }
}
