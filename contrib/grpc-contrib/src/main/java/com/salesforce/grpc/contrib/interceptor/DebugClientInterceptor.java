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

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

/**
 * {@code DebugClientInterceptor} intercepts the both outbound requests and inbound response to
 * log the method name,status,headers and message as per given logging level.
 *
 */
public class DebugClientInterceptor implements ClientInterceptor {
    private final Logger logger = LoggerFactory.getLogger(DebugClientInterceptor.class);
    private static final String REQUEST = "Request";
    private static final String RESPONSE = "Response";

    public enum Level {
        STATUS, HEADERS, MESSAGE
    }

    private EnumSet<Level> levels = EnumSet.of(Level.STATUS);

    public DebugClientInterceptor(Level... levels) {
        this.levels = EnumSet.copyOf(Arrays.asList(levels));
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void sendMessage(ReqT message) {
                logMessage(REQUEST, message);
                super.sendMessage(message);
            }

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                logMethod(method);
                logHeaders(REQUEST, headers);
                super.start(
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {

                            @Override
                            public void onClose(Status status, Metadata trailers) {
                                logStatus(status, method);
                                super.onClose(status, trailers);
                            }

                            @Override
                            public void onHeaders(Metadata headers) {
                                logHeaders(RESPONSE, headers);
                                super.onHeaders(headers);
                            }

                            @Override
                            public void onMessage(RespT message) {
                                logMessage(RESPONSE, message);
                                super.onMessage(message);
                            }
                        }, headers);
            }
        };
    }

    private <ReqT, RespT> void logMethod(MethodDescriptor<ReqT, RespT> method) {
        if (levels.contains(Level.STATUS)) {
            log(String.format("%s path : %s", REQUEST, method.getFullMethodName()));
        }
    }

    private <ReqT, RespT> void logStatus(Status status, MethodDescriptor<ReqT, RespT> method) {
        if (levels.contains(Level.STATUS)) {
            log(String.format("%s status: %s %s for path : %s", RESPONSE, status.getCode().value(), status.getCode(),
                    method.getFullMethodName()));
        }
    }

    private void logHeaders(String type, Metadata headers) {
        if (levels.contains(Level.HEADERS)) {
            log(String.format("%s headers : %s", type, headers));
        }
    }

    private <RespT> void logMessage(String type, RespT message) {
        if (levels.contains(Level.MESSAGE)) {
            log(String.format("%s message : %s", type, message));
        }
    }

    protected void log(String logmessage) {
        logger.debug(logmessage);
    }
}
