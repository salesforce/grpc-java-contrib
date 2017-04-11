/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.grpc.contrib.spring;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.Collection;

public class SimpleGrpcServerFactory implements GrpcServerFactory {
    /**
     * Override this method to override how the {@link ServerBuilder} is configured.
     */
    protected void setupServer(ServerBuilder builder) {

    }

    /**
     * Override this method to override how a {@link BindableService} is added to the {@link ServerBuilder}.
     */
    protected void registerService(ServerBuilder builder, BindableService service) {
        builder.addService(service);
    }

    @Override
    public Server buildServerForServices(int port, Collection<BindableService> services) {
        ServerBuilder builder = ServerBuilder.forPort(port);
        setupServer(builder);
        services.forEach(service -> registerService(builder, service));
        return builder.build();
    }
}
