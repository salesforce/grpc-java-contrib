/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.spring;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.Collection;

/**
 * SimpleGrpcServerFactory is a basic base class for implementing a {@link GrpcServerFactory} based on the stock gRPC
 * {@link ServerBuilder}. If you configure your implementation as a Spring {@code {@literal @}Bean},
 * {@link GrpcServerHost} will automatically pick it up.
 */
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
