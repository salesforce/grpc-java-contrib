package com.salesforce.grpc.contrib.instancemode;

import io.grpc.*;

import java.util.Collection;
import java.util.function.Supplier;

public class PerCallService<T extends BindableService> implements BindableService {
    private ServerServiceDefinition perCallBinding;

    public PerCallService(Supplier<T> factory) {
        perCallBinding = bindService(factory);
    }

    public PerCallService(Class<T> clazz) {
        this (() -> {
            try {
                return clazz.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Class " + clazz.getName() + " must have a public default constructor", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public ServerServiceDefinition bindService(Supplier<T> factory) {
        ServerServiceDefinition baseDefinition = factory.get().bindService();
        ServiceDescriptor descriptor = baseDefinition.getServiceDescriptor();
        Collection<ServerMethodDefinition<?, ?>> methods =  baseDefinition.getMethods();

        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(descriptor);
        methods.forEach(method -> builder.addMethod(ServerMethodDefinition.create(method.getMethodDescriptor(), new PerCallServerCallHandler(factory))));
        return builder.build();
    }

    @Override
    public ServerServiceDefinition bindService() {
        return perCallBinding;
    }

    private class PerCallServerCallHandler implements ServerCallHandler {
        private Supplier<T> factory;

        public PerCallServerCallHandler(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ServerCall.Listener startCall(ServerCall call, Metadata headers) {
            ServerServiceDefinition definition =  factory.get().bindService();
            ServerMethodDefinition method = definition.getMethod(call.getMethodDescriptor().getFullMethodName());
            return method.getServerCallHandler().startCall(call, headers);
        }
    }
}
