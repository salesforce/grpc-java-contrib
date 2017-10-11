/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.google.common.base.Stopwatch;
import io.grpc.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * {@code StopwatchServerInterceptor} logs the beginning and end of an inbound gRPC request, along with the total
 * processing time.
 *
 * <p>Typical usage would override {@link #logStart(MethodDescriptor)} and {@link #logStop(MethodDescriptor, Duration)}.
 */
public class StopwatchServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        logStart(call.getMethodDescriptor());

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(call, headers)) {
            private Stopwatch stopwatch = Stopwatch.createStarted();

            @Override
            public void onCancel() {
                super.onCancel();
                logCancel(call.getMethodDescriptor(), Duration.ofNanos(stopwatch.stop().elapsed(TimeUnit.NANOSECONDS)));
            }

            @Override
            public void onComplete() {
                super.onComplete();
                logStop(call.getMethodDescriptor(), Duration.ofNanos(stopwatch.stop().elapsed(TimeUnit.NANOSECONDS)));
            }
        };
    }

    /**
     * Override this method to change how "start" messages are logged. Ex: use log4j.
     *
     * @param method The operation being called
     */
    protected void logStart(MethodDescriptor method) {
        System.out.println("Begin service op:" + method.getFullMethodName());
    }

    /**
     * Override this method to change how "stop" messages are logged. Ex: use log4j.
     *
     * @param method The operation being called
     * @param duration Total round-trip time
     */
    protected void logStop(MethodDescriptor method, Duration duration) {
        System.out.println("End service op:" + method.getFullMethodName() + " duration:" + duration);
    }

    /**
     * Override this method to change how "cancel" messages are logged. Ex: use log4j.
     *
     * <p>By default, this delegates to {@link #logStop(MethodDescriptor, Duration)}.
     *
     * @param method The operation being called
     * @param duration Total round-trip time
     */
    protected void logCancel(MethodDescriptor method, Duration duration) {
        logStop(method, duration);
    }
}
