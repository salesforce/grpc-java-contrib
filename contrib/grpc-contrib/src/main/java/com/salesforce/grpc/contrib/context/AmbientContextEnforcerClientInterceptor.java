/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.*;

/**
 * Some uses of ambient context, like distributed tracing, break down if a service fails to propagate the context
 * to downstream services. This typically happens when multi-threading code fails to correctly transfer the gRPC
 * context to worker threads.
 *
 * <p>{@code AmbientContextEnforcerClientInterceptor} is used to enforce context propagation by <i>catastrophically</i>
 * failing downstream service calls if the ambient context is missing or incomplete.
 */
public class AmbientContextEnforcerClientInterceptor implements ClientInterceptor {
    private String[] requiredContextKeys = {};

    /**
     * Constructs an {@code AmbientContextEnforcerClientInterceptor} with no required context keys.
     */
    public AmbientContextEnforcerClientInterceptor() {
    }

    /**
     * Constructs an {@code AmbientContextEnforcerClientInterceptor} with a set of required context keys.
     */
    public AmbientContextEnforcerClientInterceptor(String... requiredContextKeys) {
        this.requiredContextKeys = requiredContextKeys;
    }

    /**
     * MissingAmbientContextException is thrown to indicate a breakdown in context continuity between services.
     */
    public static class MissingAmbientContextException extends RuntimeException {
        public MissingAmbientContextException(String message) {
            super(message);
        }
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        // Throw if ambient context is missing
        if (!AmbientContext.isPresent()) {
            throw missingContextException();
        }

        // Throw if required context keys are missing
        for (String requiredKey : requiredContextKeys) {
            if (!AmbientContext.current().keys().contains(requiredKey)) {
                throw incompleteContextException(requiredKey);
            }
        }

        return next.newCall(method, callOptions);
    }

    /**
     * Override this method to change the exception type or message thrown when the ambient context is missing
     * entirely, perhaps to reference your own internal documentation.
     *
     * @return a RuntimeException
     */
    protected RuntimeException missingContextException() {
        return new MissingAmbientContextException("No AmbientContext is attached to the current gRPC Context. " +
            "Make sure Context is correctly transferred between worker threads using Context.wrap() or " +
            "Context.currentContextExecutor().");
    }

    /**
     * Override this method to change the exception type or message thrown when the ambient context is missing a
     * required context key, perhaps to reference your own internal documentation.
     *
     * @return a RuntimeException
     */
    protected RuntimeException incompleteContextException(String missingKey) {
        return new MissingAmbientContextException("The AmbientContext is missing a required context key: " + missingKey);
    }
}
