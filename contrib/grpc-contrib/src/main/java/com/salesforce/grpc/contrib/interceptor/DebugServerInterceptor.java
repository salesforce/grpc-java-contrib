/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import java.util.Arrays;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;

public class DebugServerInterceptor implements ServerInterceptor {

    private final Logger logger = LoggerFactory.getLogger(DebugServerInterceptor.class);
    private static final String REQUEST = "Request";
    private static final String RESPONSE = "Response";

    public enum Level {
        METHOD, HEADERS, MESSAGE
    }

    private EnumSet<Level> levels = EnumSet.of(Level.METHOD);

    public DebugServerInterceptor(Level... levels) {
        this.levels = EnumSet.copyOf(Arrays.asList(levels));
    }

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        logMethod(REQUEST, call.getMethodDescriptor());
        logHeaders(REQUEST, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new SimpleForwardingServerCall<ReqT, RespT>(call) {

                    @Override
                    public void sendHeaders(Metadata responseHeaders) {
                        logMethod(RESPONSE, call.getMethodDescriptor());
                        logHeaders(RESPONSE, responseHeaders);
                        super.sendHeaders(responseHeaders);
                    }

                    @Override
                    public void sendMessage(RespT message) {
                        logMessage(RESPONSE, message);
                        super.sendMessage(message);
                    }
                }, headers)) {

            @Override
            public void onMessage(ReqT message) {
                logMessage(REQUEST, message);
                super.onMessage(message);
            }

            @Override
            public void onCancel() {
                log(String.format("Call for method %s cancelled", call.getMethodDescriptor().getFullMethodName()));
                super.onCancel();

            }

        };
    }

    private <ReqT, RespT> void logMethod(String type, MethodDescriptor<ReqT, RespT> method) {
        if (levels.contains(Level.METHOD)) {
            log(String.format("%s path : %s", type, method.getFullMethodName()));
        }
    }

    private void logHeaders(String type, Metadata headers) {
        if (levels.contains(Level.HEADERS)) {
            log(String.format("%s headers : %s", type, headers));
        }
    }

    private <T> void logMessage(String type, T message) {
        if (levels.contains(Level.MESSAGE)) {
            log(String.format("%s message : %s", type, message));
        }
    }

    protected void log(String logmessage) {
        logger.debug(logmessage);
    }

}
