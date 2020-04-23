/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

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
        NONE, STATUS, HEADERS, MESSAGE
    }

    private Level level = Level.NONE;

    public DebugClientInterceptor(Level level) {
        this.level = level;
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
                            /**
                             * for logging level of {@code HEADERS} or {@code MESSAGE}, the status will be
                             * logged after headers and message
                             */
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

    /**
     * override this method to change the way outbound request method name is logged.
     */
    protected <ReqT, RespT> void logMethod(MethodDescriptor<ReqT, RespT> method) {
        if (level == Level.STATUS || level == Level.HEADERS || level == Level.MESSAGE) {
            logger.debug("{} path : {}", REQUEST, method.getFullMethodName());
        }
    }

    /**
     * override this method to change the way status and method name is logged for inbound response.
     */
    protected <ReqT, RespT> void logStatus(Status status, MethodDescriptor<ReqT, RespT> method) {
        if (level == Level.STATUS || level == Level.HEADERS || level == Level.MESSAGE) {
            logger.debug("{} status: {} {} for path :{}", RESPONSE, status.getCode().value(), status.getCode(),
                    method.getFullMethodName());
        }
    }
    /**
     * override this method to change the way headers are logged.
     */
    protected void logHeaders(String type, Metadata headers) {
        if (level == Level.HEADERS || level == Level.MESSAGE) {
            logger.debug("{} headers : {}", type, headers);
        }
    }
    /**
     * override this method to change the way message is logged.
     */
    protected <RespT> void logMessage(String type, RespT message) {
        if (this.level == Level.MESSAGE) {
            logger.debug("{} message : {}", type, message);
        }
    }
}
