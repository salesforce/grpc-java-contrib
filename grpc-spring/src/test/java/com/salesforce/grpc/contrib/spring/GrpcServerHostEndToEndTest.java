/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.spring;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GrpcServerHostEndToEndTest {

    @Autowired
    private GrpcServerHost grpcServerHost;

    @Test
    public void serverIsRunningAndSayHelloReturnsExpectedResponse() throws Exception {
        final String name = UUID.randomUUID().toString();
        grpcServerHost.start();

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", grpcServerHost.getPort())
                .usePlaintext(true)
                .build();

        GreeterGrpc.GreeterFutureStub stub = GreeterGrpc.newFutureStub(channel);

        ListenableFuture<HelloResponse> responseFuture = stub.sayHello(HelloRequest.newBuilder().setName(name).build());
        AtomicReference<HelloResponse> response = new AtomicReference<>();

        Futures.addCallback(
                responseFuture,
                new FutureCallback<HelloResponse>() {
                    @Override
                    public void onSuccess(@Nullable HelloResponse result) {
                        response.set(result);
                    }

                    @Override
                    public void onFailure(Throwable t) {

                    }
                },
                MoreExecutors.directExecutor());

        await().atMost(10, TimeUnit.SECONDS).until(responseFuture::isDone);

        channel.shutdownNow();

        assertThat(response.get()).isNotNull();
        assertThat(response.get().getMessage()).contains(name);
    }

    private interface GreetingComposer {
        String greet(String name);
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        public GreeterImpl greeter() {
            return new GreeterImpl();
        }

        @Bean
        public GreetingComposer greetingComposer() {
            return name -> "Hello, " + name;
        }

        @Bean
        public GrpcServerFactory factory() {
            return new SimpleGrpcServerFactory() {
                @Override
                public Server buildServerForServices(int port, Collection<BindableService> services) {
                    System.out.println("Building a service for " + services.size() + " services");
                    return super.buildServerForServices(port, services);
                }

                @Override
                public List<Class<? extends Annotation>> forAnnotations() {
                    return ImmutableList.of(GrpcService.class, AlsoAGrpcService.class);
                }
            };
        }

        @Bean
        public GrpcServerHost serverHost() throws IOException {
            return new GrpcServerHost(9999);
        }
    }

    @GrpcService
    @AlsoAGrpcService
    private static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Autowired
        private GreetingComposer composer;

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            responseObserver.onNext(HelloResponse.newBuilder().setMessage(composer.greet(request.getName())).build());
            responseObserver.onCompleted();
        }
    }
}
