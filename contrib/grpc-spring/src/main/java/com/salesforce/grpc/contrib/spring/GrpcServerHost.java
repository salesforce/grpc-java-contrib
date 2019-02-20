/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.spring;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import io.grpc.BindableService;
import io.grpc.Server;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * {@code GrpcServerHost} configures a gRPC {@link Server} with services obtained from the {@link ApplicationContext}
 * and manages that server's lifecycle. Services are discovered by finding {@link BindableService} implementations that
 * are annotated with {@link GrpcService}.
 */
public class GrpcServerHost implements AutoCloseable, ApplicationContextAware {

    static final int MAX_PORT = 65535;
    static final int MIN_PORT = 0;
    static final int DEFAULT_SHUTDOWN_DELAY_SECONDS = 3;


    @VisibleForTesting
    ApplicationContext applicationContext;

    @VisibleForTesting
    GrpcServerFactory serverFactory;

    @VisibleForTesting
    final int port;

    @VisibleForTesting
    final long shutdownWaitTimeInMillis;

    @VisibleForTesting
    final Server server() {
        return server;
    }

    private volatile Server server;

    /**
     * Construct a GrpcServerHost on a given port with a three second shutdown timeout.
     * @param port The port to listen on
     */
    public GrpcServerHost(int port) {
        this(port, TimeUnit.SECONDS.toMillis(DEFAULT_SHUTDOWN_DELAY_SECONDS));
    }

    /**
     * Construct a GrpcServerHost on a given port with a given shutdown timeout.
     * @param port The port to listen on
     * @param shutdownWaitTimeInMillis Timeout for shutdown
     */
    public GrpcServerHost(int port, long shutdownWaitTimeInMillis) {
        this(port, shutdownWaitTimeInMillis, null);
    }

    /**
     * Construct a GrpcServerHost on a given port with a given shutdown timeout and {@link GrpcServerFactory}.
     * @param port The port to listen on
     * @param shutdownWaitTimeInMillis Timeout for shutdown
     * @param serverFactory A factory to construct gRPC {@link Server} instances
     */
    public GrpcServerHost(int port, long shutdownWaitTimeInMillis, GrpcServerFactory serverFactory) {
        Preconditions.checkArgument(
                port >= MIN_PORT && port <= MAX_PORT,
                "port must be between %s and %s, inclusive",
                MIN_PORT,
                MAX_PORT);

        Preconditions.checkArgument(
                shutdownWaitTimeInMillis >= 0,
                "shutdownWaitTimeInMillis must be greater than or equal to 0");

        this.port = port;
        this.shutdownWaitTimeInMillis = shutdownWaitTimeInMillis;
        this.serverFactory = serverFactory;
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = Preconditions.checkNotNull(applicationContext);
    }

    /**
     * Returns the actual port of the running gRPC server. The port is only available once the server is up and running.
     *
     * @throws IllegalStateException if called either before the server has started or after it has stopped
     */
    public final int getPort() {
        final Server server = server();

        if (server == null) {
            throw new IllegalStateException("Cannot fetch port until server has started.");
        }

        return server.getPort();
    }

    /**
     * Start the gRPC {@link Server}.
     *
     * @throws IOException if unable to bind to server address or port
     * @throws IllegalStateException if any non-{@link BindableService} classes are annotated with {@link GrpcService}
     */
    public void start() throws IOException {
        if (serverFactory == null) {
            serverFactory = findServerFactory();
        }

        final Collection<BindableService> services = getServicesFromApplicationContext();

        if (services.isEmpty()) {
            throw new IOException("gRPC server not started because no services were found in the application context.");
        }

        server = serverFactory.buildServerForServices(port, services);
        server.start();
    }

    private GrpcServerFactory findServerFactory() {
        // Find a GrpcServerFactory implementation from the applicationContext. Use the default SimpleGrpcServerFactory
        // if no alternative is found.
        Map<String, GrpcServerFactory> factoryMap = applicationContext.getBeansOfType(GrpcServerFactory.class);
        if (factoryMap.isEmpty()) {
            return new SimpleGrpcServerFactory();
        } else {
            // Do a single bean lookup so we don't have to implement thinks like @Primary or FactoryBean<T>
            // May throw NoUniqueBeanDefinitionException if multiple implementation beans are found
            return applicationContext.getBean(GrpcServerFactory.class);
        }
    }

    /**
     * Shutdown the gRPC {@link Server} when this object is closed.
     */
    @Override
    public void close() throws Exception {
        final Server server = server();

        if (server != null) {
            server.shutdown();

            try {
                // TODO: Maybe we should catch the InterruptedException from this?
                server.awaitTermination(shutdownWaitTimeInMillis, TimeUnit.MILLISECONDS);
            } finally {
                server.shutdownNow();

                this.server = null;
            }
        }
    }

    private Collection<BindableService> getServicesFromApplicationContext() {
        Map<String, Object> possibleServices = new HashMap<>();

        for (Class<? extends Annotation> annotation : serverFactory.forAnnotations()) {
            possibleServices.putAll(applicationContext.getBeansWithAnnotation(annotation));
        }

        Collection<String> invalidServiceNames = possibleServices.entrySet().stream()
                .filter(e -> !(e.getValue() instanceof BindableService))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!invalidServiceNames.isEmpty()) {
            throw new IllegalStateException((format(
                    "The following beans are annotated with @GrpcService, but are not BindableServices: %s",
                    String.join(", ", invalidServiceNames))));
        }

        return possibleServices.values().stream().map(s -> (BindableService) s).collect(Collectors.toList());
    }
}
