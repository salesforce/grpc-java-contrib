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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 *
 * @param <T>
 */
public class PerSessionService<T extends BindableService> implements BindableService {
    public static Context.Key<UUID> SESSION_ID = Context.key("SESSION_ID");

    private ServerServiceDefinition perSessionBinding;
    private Map<UUID, T> sessionServices = new ConcurrentHashMap<>();

    /**
     *
     * @param factory
     */
    public PerSessionService(Supplier<T> factory) {
        PerSessionServerTransportFilter.subscribeToTerminated((o, arg) -> deactivate((UUID) arg));
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
            UUID sessionId = call.getAttributes().get(PerSessionServerTransportFilter.SESSION_ID);
            if (sessionId != null) {
                Context sessionIdContext = Context.current().withValue(SESSION_ID, sessionId);

                if (!sessionServices.containsKey(sessionId)) {
                    T instance = factory.get();
                    sessionServices.put(sessionId, instance);

                    ServerServiceDefinition definition = instance.bindService();
                    ServerMethodDefinition method = definition.getMethod(call.getMethodDescriptor().getFullMethodName());

                    return Contexts.interceptCall(sessionIdContext, call, headers, method.getServerCallHandler());
                } else {
                    T instance = sessionServices.get(sessionId);
                    ServerServiceDefinition definition = instance.bindService();
                    ServerMethodDefinition method = definition.getMethod(call.getMethodDescriptor().getFullMethodName());

                    return Contexts.interceptCall(sessionIdContext, call, headers, method.getServerCallHandler());
                }
            } else {
                throw new IllegalStateException("PerSessionServerTransportFilter was not registered with " +
                        "ServerBuilder.addTransportFilter(new PerSessionServerTransportFilter())");
            }
        }
    }

    private void deactivate(UUID sessionKey) {
        T instance = sessionServices.remove(sessionKey);
        if (instance instanceof AutoCloseable) {
            try {
                ((AutoCloseable) instance).close();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
}
