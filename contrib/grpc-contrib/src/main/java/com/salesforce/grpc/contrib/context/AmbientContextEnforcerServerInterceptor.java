/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.*;

/**
 * Some uses of ambient context, like distributed tracing, break down if a service fails to propagate the context
 * to downstream services.
 *
 * <p>{@code AmbientContextEnforcerServerInterceptor} is used to enforce context propagation by <i>catastrophically</i>
 * failing a service call if the ambient context is missing or incomplete.
 */
public class AmbientContextEnforcerServerInterceptor implements ServerInterceptor {
    private String[] requiredContextKeys = {};

    /**
     * Constructs an {@code AmbientContextEnforcerServerInterceptor} with no required context keys.
     */
    public AmbientContextEnforcerServerInterceptor() {
    }

    /**
     * Constructs an {@code AmbientContextEnforcerServerInterceptor} with a set of required context keys.
     */
    public AmbientContextEnforcerServerInterceptor(String... requiredContextKeys) {
        this.requiredContextKeys = requiredContextKeys;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        // Throw if ambient context is missing
        if (!AmbientContext.isPresent()) {
            call.close(missingContextStatus(), new Metadata());
            return new ServerCall.Listener<ReqT>() { };
        }

        // Throw if required context keys are missing
        for (String requiredKey : requiredContextKeys) {
            if (!AmbientContext.current().keys().contains(requiredKey)) {
                call.close(incompleteContextStatus(requiredKey), new Metadata());
                return new ServerCall.Listener<ReqT>() { };
            }
        }

        return next.startCall(call, headers);
    }

    /**
     * Override this method to change the gRPC {@code Status} returned when the ambient context is missing
     * entirely, perhaps to reference your own internal documentation.
     *
     * @return a gRPC Status
     */
    protected Status missingContextStatus() {
        return Status.FAILED_PRECONDITION.withDescription("Ambient context is required but not found");
    }

    /**
     * Override this method to change the gRPC {@code Status} returned when the ambient context is missing a
     * required context key, perhaps to reference your own internal documentation.
     *
     * @return a gRPC Status
     */
    protected Status incompleteContextStatus(String missingKey) {
        return Status.FAILED_PRECONDITION.withDescription("Required ambient context key " + missingKey + " was not found");
    }
}
