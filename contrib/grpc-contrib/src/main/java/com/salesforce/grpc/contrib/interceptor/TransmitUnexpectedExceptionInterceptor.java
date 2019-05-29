/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import io.grpc.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A class that intercepts uncaught exceptions of all types and handles them by closing the {@link ServerCall}, and
 * transmitting the exception's description and stack trace to the client. This class is a complement to gRPC's
 * {@code TransmitStatusRuntimeExceptionInterceptor}.
 *
 * <p>Without this interceptor, gRPC will strip all details and close the {@link ServerCall} with
 * a generic {@link Status#UNKNOWN} code.
 *
 * <p>Security warning: the exception description and stack trace may contain sensitive server-side
 * state information, and generally should not be sent to clients. Only install this interceptor
 * if all clients are trusted.
 */
// Heavily inspired by https://github.com/saturnism/grpc-java-by-example/blob/master/error-handling-example/error-server/src/main/java/com/example/grpc/server/UnknownStatusDescriptionInterceptor.java
public class TransmitUnexpectedExceptionInterceptor implements ServerInterceptor {

    private final Set<Class<? extends Throwable>> exactTypes = new HashSet<>();
    private final Set<Class<? extends Throwable>> parentTypes = new HashSet<>();

    /**
     * Allows this interceptor to match on an exact exception type.
     * @param exactType The exact type to match on.
     * @return this
     */
    public TransmitUnexpectedExceptionInterceptor forExactType(Class<? extends Throwable> exactType) {
        this.exactTypes.add(exactType);
        return this;
    }

    /**
     * Allows this interceptor to match on a set of exact exception type.
     * @param exactTypes The set of exact types to match on.
     * @return this
     */
    public TransmitUnexpectedExceptionInterceptor forExactTypes(Collection<Class<? extends Throwable>> exactTypes) {
        this.exactTypes.addAll(exactTypes);
        return this;
    }

    /**
     * Allows this interceptor to match on any exception type deriving from {@code parentType}.
     * @param parentType The parent type to match on.
     * @return this
     */
    public TransmitUnexpectedExceptionInterceptor forParentType(Class<? extends Throwable> parentType) {
        this.parentTypes.add(parentType);
        return this;
    }

    /**
     * Allows this interceptor to match on any exception type deriving from any element of {@code parentTypes}.
     * @param parentTypes The set of parent types to match on.
     * @return this
     */
    public TransmitUnexpectedExceptionInterceptor forParentTypes(Collection<Class<? extends Throwable>> parentTypes) {
        this.parentTypes.addAll(parentTypes);
        return this;
    }

    /**
     * Allows this interceptor to match all exceptions. Use with caution!
     * @return this
     */
    public TransmitUnexpectedExceptionInterceptor forAllExceptions() {
        return forParentType(Throwable.class);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void sendMessage(RespT message) {
                super.sendMessage(message);
            }

            @Override
            public void close(Status status, Metadata trailers) {

                if (status.getCode() == Status.Code.UNKNOWN &&
                        status.getDescription() == null &&
                        status.getCause() != null &&
                        exceptionTypeIsAllowed(status.getCause().getClass())) {
                    Throwable e = status.getCause();
                    status = Status.INTERNAL
                            .withDescription(e.getMessage())
                            .augmentDescription(stacktraceToString(e));
                }
                super.close(status, trailers);
            }
        };

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(wrappedCall, headers)) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Throwable e) {
                    if (exceptionTypeIsAllowed(e.getClass())) {
                        call.close(Status.INTERNAL
                                .withDescription(e.getMessage())
                                .augmentDescription(stacktraceToString(e)), new Metadata());
                    } else {
                        throw e;
                    }
                }
            }
        };
    }

    private boolean exceptionTypeIsAllowed(Class<? extends Throwable> exceptionClass) {
        // exact matches
        for (Class<? extends Throwable> clazz : exactTypes) {
            if (clazz.equals(exceptionClass)) {
                return true;
            }
        }

        // parent type matches
        for (Class<? extends Throwable> clazz : parentTypes) {
            if (clazz.isAssignableFrom(exceptionClass)) {
                return true;
            }
        }

        // no match
        return false;
    }

    private String stacktraceToString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}