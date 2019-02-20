/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.google.common.base.Stopwatch;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * {@code StopwatchClientInterceptor} logs the beginning and end of an outbound gRPC request, along with the total
 * round-trip time.
 *
 * <p>Typical usage would override {@link #logStart(MethodDescriptor)} and {@link #logStop(MethodDescriptor, Duration)}.
 */
public class StopwatchClientInterceptor implements ClientInterceptor {
    private final Logger logger = LoggerFactory.getLogger(StopwatchClientInterceptor.class);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        logStart(method);

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            private Stopwatch stopwatch = Stopwatch.createStarted();

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        super.onClose(status, trailers);
                        logStop(method, Duration.ofNanos(stopwatch.stop().elapsed(TimeUnit.NANOSECONDS)));
                    }
                }, headers);
            }
        };
    }

    /**
     * Override this method to change how start messages are logged. Ex: use log4j.
     *
     * @param method The operation being called
     */
    protected void logStart(MethodDescriptor method) {
        logger.info("Begin call op:" + method.getFullMethodName());
    }

    /**
     * Override this method to change how stop messages are logged. Ex: use log4j.
     *
     * @param method The operation being called
     * @param duration Total round-trip time
     */
    protected void logStop(MethodDescriptor method, Duration duration) {
        logger.info("End call op:" + method.getFullMethodName() + " duration:" + duration);
    }
}
