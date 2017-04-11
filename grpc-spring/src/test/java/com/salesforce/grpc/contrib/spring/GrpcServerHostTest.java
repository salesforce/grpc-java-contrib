/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.spring;/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

import com.google.common.collect.ImmutableMap;
import io.grpc.BindableService;
import io.grpc.Server;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.mockito.internal.stubbing.defaultanswers.TriesToReturnSelf;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.salesforce.grpc.contrib.spring.GrpcServerHost.MAX_PORT;
import static com.salesforce.grpc.contrib.spring.GrpcServerHost.MIN_PORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GrpcServerHostTest {

    @Test
    public void constructorThrowsForPortLessThanMin() {
        final int port = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, MIN_PORT);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        assertThatThrownBy(() -> new GrpcServerHost(port, shutdownWaitTimeInMillis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }

    @Test
    public void constructorThrowsForPortGreaterThanMax() {
        final int port = ThreadLocalRandom.current().nextInt(MAX_PORT, Integer.MAX_VALUE);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        assertThatThrownBy(() -> new GrpcServerHost(port, shutdownWaitTimeInMillis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }

    @Test
    public void constructorThrowsForNegativeShutdownWaitTimeInMillis() {
        final int port = ThreadLocalRandom.current().nextInt(1000, 10000);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(Integer.MIN_VALUE, 0);

        assertThatThrownBy(() -> new GrpcServerHost(port, shutdownWaitTimeInMillis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shutdownWaitTimeInMillis");
    }

    @Test
    public void constructorSetsFieldsCorrectly() {
        final int port = ThreadLocalRandom.current().nextInt(1000, 10000);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        final ApplicationContext applicationContext = mock(ApplicationContext.class);

        GrpcServerHost runner = new GrpcServerHost(port, shutdownWaitTimeInMillis);
        runner.setApplicationContext(applicationContext);

        assertThat(runner.applicationContext).isSameAs(applicationContext);
        assertThat(runner.port).isEqualTo(port);
        assertThat(runner.shutdownWaitTimeInMillis).isEqualTo(shutdownWaitTimeInMillis);
    }

    @Test
    public void startThrowsForBadServices() {
        final String badService1 = UUID.randomUUID().toString();
        final String badService2 = UUID.randomUUID().toString();
        final String goodService = UUID.randomUUID().toString();
        final int port = ThreadLocalRandom.current().nextInt(1000, 10000);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        final ApplicationContext applicationContext = mock(ApplicationContext.class);

        final Map<String, Object> services = ImmutableMap.of(
                badService1, new Object(),
                badService2, new Object(),
                goodService, new GreeterGrpc.GreeterImplBase() { });

        when(applicationContext.getBeansWithAnnotation(eq(GrpcService.class))).thenReturn(services);

        GrpcServerHost runner = new GrpcServerHost(port, shutdownWaitTimeInMillis);
        runner.setApplicationContext(applicationContext);

        assertThatThrownBy(runner::start)
                .isInstanceOf(IllegalStateException.class)
                .doesNotHave(new Condition<>(
                        t -> t.getMessage().contains(goodService),
                        "Error should not include good service."))
                .hasMessageContaining(badService1)
                .hasMessageContaining(badService2);
    }

    @Test
    public void startStartsServerWithServices() throws Exception {
        final int port = ThreadLocalRandom.current().nextInt(1000, 10000);
        final int serviceCount = ThreadLocalRandom.current().nextInt(5, 10);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        final ApplicationContext applicationContext = mock(ApplicationContext.class);
        final Server server = mock(Server.class, new TriesToReturnSelf());
        when(server.getPort()).thenReturn(port);

        final Map<String, Object> services = IntStream.range(0, serviceCount)
                .mapToObj(i -> mock(BindableService.class))
                .collect(Collectors.toMap(s -> UUID.randomUUID().toString(), s -> s));

        AtomicBoolean built = new AtomicBoolean(false);

        GrpcServerFactory fakeFactory = (p, s) -> {
            built.set(true);
            assertThat(p).isEqualTo(port);
            s.forEach(ss -> assertThat(services.values().contains(ss)).isTrue());
            return server;
        };

        when(applicationContext.getBeansWithAnnotation(eq(GrpcService.class))).thenReturn(services);

        GrpcServerHost runner = new GrpcServerHost(port, shutdownWaitTimeInMillis, fakeFactory);
        runner.setApplicationContext(applicationContext);

        runner.start();
        assertThat(built.get()).isTrue();

        verify(server).start();

        assertThat(runner.server()).isEqualTo(server);
    }

    @Test
    public void startDoesNotStartServerWithoutServices() throws Exception {
        final int port = ThreadLocalRandom.current().nextInt(1000, 10000);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        final ApplicationContext applicationContext = mock(ApplicationContext.class);
        final Server server = mock(Server.class, new TriesToReturnSelf());
        final GrpcServerFactory factory = mock(GrpcServerFactory.class);

        when(server.getPort()).thenReturn(port);

        // Configure application context to contain no gRPC services.
        when(applicationContext.getBeansWithAnnotation(eq(GrpcService.class))).thenReturn(ImmutableMap.of());

        GrpcServerHost runner = new GrpcServerHost(port, shutdownWaitTimeInMillis, factory);
        runner.setApplicationContext(applicationContext);

        assertThatThrownBy(runner::start).isInstanceOf(IOException.class);

        // Make sure the server builder was not used.
        verifyZeroInteractions(factory);

        assertThat(runner.server()).isNull();
    }

    @Test
    public void closeStopsRunningServer() throws Exception {
        final int port = ThreadLocalRandom.current().nextInt(1000, 10000);
        final int serviceCount = ThreadLocalRandom.current().nextInt(5, 10);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        final ApplicationContext applicationContext = mock(ApplicationContext.class);
        final Server server = mock(Server.class, new TriesToReturnSelf());

        when(server.getPort()).thenReturn(port);

        final Map<String, Object> services = IntStream.range(0, serviceCount)
                .mapToObj(i -> mock(BindableService.class))
                .collect(Collectors.toMap(s -> UUID.randomUUID().toString(), s -> s));

        final GrpcServerFactory factory = (p, s) -> server;

        when(applicationContext.getBeansWithAnnotation(eq(GrpcService.class))).thenReturn(services);

        GrpcServerHost runner = new GrpcServerHost(port, shutdownWaitTimeInMillis, factory);
        runner.setApplicationContext(applicationContext);

        runner.start();

        assertThat(runner.server()).isEqualTo(server);

        runner.close();

        verify(server).shutdown();
        verify(server).awaitTermination(eq(shutdownWaitTimeInMillis), eq(TimeUnit.MILLISECONDS));
        verify(server).shutdownNow();

        assertThat(runner.server()).isNull();
    }

    @Test
    public void closeDoesNothingForStoppedServer() throws Exception {
        final int port = ThreadLocalRandom.current().nextInt(1000, 10000);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        GrpcServerHost runner = new GrpcServerHost(
                port,
                shutdownWaitTimeInMillis);

        assertThat(runner.server()).isNull();

        runner.close();

        assertThat(runner.server()).isNull();
    }

    @Test
    public void getPortThrowsIfServerIsNotRunning() {
        final int port = ThreadLocalRandom.current().nextInt(1000, 10000);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        GrpcServerHost runner = new GrpcServerHost(
                port,
                shutdownWaitTimeInMillis);

        assertThatThrownBy(runner::getPort)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getPortReturnsServerPortForRunningServer() throws Exception {
        final int configPort = ThreadLocalRandom.current().nextInt(1000, 2000);
        final int serverPort = ThreadLocalRandom.current().nextInt(2000, 3000);
        final int serviceCount = ThreadLocalRandom.current().nextInt(5, 10);
        final long shutdownWaitTimeInMillis = ThreadLocalRandom.current().nextLong(1000, 10000);

        final ApplicationContext applicationContext = mock(ApplicationContext.class);
        final Server server = mock(Server.class, new TriesToReturnSelf());
        final GrpcServerFactory factory = (p, s) -> server;

        final Map<String, Object> services = IntStream.range(0, serviceCount)
                .mapToObj(i -> mock(BindableService.class))
                .collect(Collectors.toMap(s -> UUID.randomUUID().toString(), s -> s));

        when(applicationContext.getBeansWithAnnotation(eq(GrpcService.class))).thenReturn(services);

        when(server.getPort()).thenReturn(serverPort);

        GrpcServerHost runner = new GrpcServerHost(configPort, shutdownWaitTimeInMillis, factory);
        runner.setApplicationContext(applicationContext);

        runner.start();

        assertThat(runner.getPort()).isEqualTo(serverPort);
    }
}
