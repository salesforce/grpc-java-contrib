/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.instancemode;

import io.grpc.*;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 *
 * @param <T>
 */
public class PerSessionService<T extends BindableService> implements BindableService {
    private ServerServiceDefinition perSessionBinding;
    private Map<UUID, ServerCall.Listener> sessionServices = new WeakHashMap<>();

    /**
     *
     * @param factory
     */
    public PerSessionService(Supplier<T> factory) {
        PerSessionServerTransportFilter.subscribeToTerminated((o, arg) -> sessionServices.remove(arg));
        perSessionBinding = bindService(factory);
    }

    /**
     *
     * @param clazz
     */
    public PerSessionService(Class<T> clazz) {
        this (() -> {
            try {
                return clazz.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Class " + clazz.getName() + " must have a public default constructor", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private ServerServiceDefinition bindService(Supplier<T> factory) {
        ServerServiceDefinition baseDefinition = factory.get().bindService();
        ServiceDescriptor descriptor = baseDefinition.getServiceDescriptor();
        Collection<ServerMethodDefinition<?, ?>> methods =  baseDefinition.getMethods();

        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(descriptor);
        methods.forEach(method -> builder.addMethod(ServerMethodDefinition.create(method.getMethodDescriptor(), new PerSessionServerCallHandler(factory))));
        return builder.build();
    }

    @Override
    public ServerServiceDefinition bindService() {
        return perSessionBinding;
    }

    /**
     *
     */
    private class PerSessionServerCallHandler implements ServerCallHandler {
        private Supplier<T> factory;

        PerSessionServerCallHandler(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ServerCall.Listener startCall(ServerCall call, Metadata headers) {
            UUID sessionKey = call.getAttributes().get(PerSessionServerTransportFilter.PER_SESSION_KEY);
            if (sessionKey != null) {
                if (!sessionServices.containsKey(sessionKey)) {
                    ServerServiceDefinition definition = factory.get().bindService();
                    ServerMethodDefinition method = definition.getMethod(call.getMethodDescriptor().getFullMethodName());
                    sessionServices.put(sessionKey, method.getServerCallHandler().startCall(call, headers));
                }
                return sessionServices.get(sessionKey);
            } else {
                throw new IllegalStateException("PerSessionServerTransportFilter was not registered with " +
                        "ServerBuilder.addTransportFilter()");
            }
        }
    }
}
