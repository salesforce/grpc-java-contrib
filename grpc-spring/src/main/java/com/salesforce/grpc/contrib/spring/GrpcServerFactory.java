/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.spring;

import com.google.common.collect.ImmutableList;
import io.grpc.BindableService;
import io.grpc.Server;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

/**
 * Implement this interface in a bean to override how {@link GrpcServerHost} initializes a {@link Server} from a
 * collection of {@link BindableService}s. Using a GrpcServerFactory, you can configure things like TLS settings
 * and {@link io.grpc.ServerInterceptor}s.
 */
public interface GrpcServerFactory {
    /**
     * Constructs a {@link Server} from a collection of {@link BindableService}s attached to the given port.
     * @param port The port to use for the {@link Server}
     * @param services The list of {@link BindableService}s to host
     * @return A new grpc {@link Server}
     */
    Server buildServerForServices(int port, Collection<BindableService> services);

    /**
     * The {@link Annotation}s this GrpcServerFactory will match on when discovering gRPC service implementations.
     * Override this method to provide your own set of annotations instead of the default
     * {@code {@literal @}GrpcService} annotation.
     *
     * @return a set of java annotations to match on.
     */
    default List<Class<? extends Annotation>> forAnnotations() {
        return ImmutableList.of(GrpcService.class);
    }
}
