/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.testing.contrib;

import com.salesforce.grpc.contrib.Servers;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import org.junit.rules.ExternalResource;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * {@code NettyGrpcServerRule} is a JUnit {@link org.junit.rules.TestRule} that starts a gRPC Netty service with
 * a {@link MutableHandlerRegistry} for adding services. It is particularly useful for testing middleware and
 * interceptors using the "real" gRPC wire protocol instead of the InProcess protocol. While InProcess testing works
 * 99% of the time, the Netty and InProcess transports have different flow control and serialization semantics that
 * can have an affect on low-level gRPC integrations.
 *
 * <p>An {@link io.grpc.stub.AbstractStub} can be created against this service by using the
 * {@link ManagedChannel} provided by {@link NettyGrpcServerRule#getChannel()}.
 */
public class NettyGrpcServerRule extends ExternalResource {

    private ManagedChannel channel;
    private Server server;
    private MutableHandlerRegistry serviceRegistry;
    private boolean useDirectExecutor;
    private int port = 0;

    private Consumer<NettyServerBuilder> configureServerBuilder = sb -> { };
    private Consumer<NettyChannelBuilder> configureChannelBuilder = cb -> { };

    /**
     * Provides a way to configure the {@code NettyServerBuilder} used for testing.
     */
    public final NettyGrpcServerRule configureServerBuilder(Consumer<NettyServerBuilder> configureServerBuilder) {
        checkState(port == 0, "configureServerBuilder() can only be called at the rule instantiation");
        this.configureServerBuilder = checkNotNull(configureServerBuilder, "configureServerBuilder");
        return this;
    }

    /**
     * Provides a way to configure the {@code NettyChannelBuilder} used for testing.
     */
    public final NettyGrpcServerRule configureChannelBuilder(Consumer<NettyChannelBuilder> configureChannelBuilder) {
        checkState(port == 0, "configureChannelBuilder() can only be called at the rule instantiation");
        this.configureChannelBuilder = checkNotNull(configureChannelBuilder, "configureChannelBuilder");
        return this;
    }

    /**
     * Returns a {@link ManagedChannel} connected to this service.
     */
    public final ManagedChannel getChannel() {
        return channel;
    }

    /**
     * Returns the underlying gRPC {@link Server} for this service.
     */
    public final Server getServer() {
        return server;
    }

    /**
     * Returns the randomly generated TCP port for this service.
     */
    public final int getPort() {
        return port;
    }

    /**
     * Returns the service registry for this service. The registry is used to add service instances
     * (e.g. {@link io.grpc.BindableService} or {@link io.grpc.ServerServiceDefinition} to the server.
     */
    public final MutableHandlerRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Before the test has started, create the server and channel.
     */
    @Override
    protected void before() throws Throwable {
        serviceRegistry = new MutableHandlerRegistry();

        NettyServerBuilder serverBuilder = NettyServerBuilder
                .forPort(0)
                .fallbackHandlerRegistry(serviceRegistry);

        if (useDirectExecutor) {
            serverBuilder.directExecutor();
        }

        configureServerBuilder.accept(serverBuilder);
        server = serverBuilder.build().start();
        port = server.getPort();

        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("localhost", port).usePlaintext(true);
        configureChannelBuilder.accept(channelBuilder);
        channel = channelBuilder.build();
    }

    /**
     * After the test has completed, clean up the channel and server.
     */
    @Override
    protected void after() {
        serviceRegistry = null;

        channel.shutdown();
        channel = null;
        port = 0;

        try {
            Servers.shutdownGracefully(server, 1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            server = null;
        }
    }
}
