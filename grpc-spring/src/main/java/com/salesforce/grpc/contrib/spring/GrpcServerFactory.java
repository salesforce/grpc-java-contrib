/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.grpc.contrib.spring;

import io.grpc.BindableService;
import io.grpc.Server;

import java.util.Collection;

/**
 * Implement this interface in a bean to override how {@link GrpcServerHost} initializes a {@link Server} from a
 * collection of {@link BindableService}s. Using a GrpcServerFactory, you can configure things like TLS settings
 * and {@link io.grpc.ServerInterceptor}s.
 */
public interface GrpcServerFactory {
    /**
     * Constructs a {@link Server} from a collection of {@link BindableService}s attached to the given port
     * @param port The port to use for the {@link Server}
     * @param services The list of {@link BindableService}s to host
     * @return A new grpc {@link Server}
     */
    Server buildServerForServices(int port, Collection<BindableService> services);
}
