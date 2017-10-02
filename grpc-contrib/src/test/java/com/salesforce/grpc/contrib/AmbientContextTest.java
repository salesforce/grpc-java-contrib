/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.util.concurrent.ListenableFuture;
import com.salesforce.grpc.contrib.context.AmbientContext;
import com.salesforce.grpc.contrib.context.AmbientContextClientInterceptor;
import com.salesforce.grpc.contrib.context.AmbientContextServerInterceptor;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.awaitility.Duration;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SuppressWarnings("Duplicates")
public class AmbientContextTest {
    @Test
    public void initializeAttachesContext() {
        Context ctx = AmbientContext.initialize(Context.current());
        ctx.run(() -> assertThat(AmbientContext.current()).isNotNull());
    }

    @Test
    public void uninitializedContextThrows() {
        assertThatThrownBy(AmbientContext::current).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void contextTransfersOneHopSync() throws Exception {
        Metadata.Key<String> ctxKey = Metadata.Key.of("ctx-context-key", Metadata.ASCII_STRING_MARSHALLER);
        String expectedCtxValue = "context-value";
        AtomicReference<String> ctxValue = new AtomicReference<>();

        // Service
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                ctxValue.set(AmbientContext.current().get(ctxKey));
                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }
        };

        // Plumbing
        Server server = ServerBuilder
                .forPort(0)
                .addService(svc)
                .intercept(new AmbientContextServerInterceptor("ctx-"))
                .build()
                .start();
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", server.getPort())
                .usePlaintext(true)
                .intercept(new AmbientContextClientInterceptor("ctx-"))
                .build();
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

        // Test
        try {
            AmbientContext.initialize(Context.current()).run(() -> {
                AmbientContext.current().put(ctxKey, expectedCtxValue);
                stub.sayHello(HelloRequest.newBuilder().setName("world").build());
            });

            assertThat(ctxValue.get()).isEqualTo(expectedCtxValue);
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    public void contextTransfersTwoHopSync() throws Exception {
        Metadata.Key<String> ctxKey = Metadata.Key.of("ctx-context-key", Metadata.ASCII_STRING_MARSHALLER);
        String expectedCtxValue = "context-value";
        AtomicReference<String> ctxValue = new AtomicReference<>();

        // Terminal service
        GreeterGrpc.GreeterImplBase svc2 = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                ctxValue.set(AmbientContext.current().get(ctxKey));
                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }
        };

        // Terminal service plumbing
        Server server2 = ServerBuilder
                .forPort(0)
                .addService(svc2)
                .intercept(new AmbientContextServerInterceptor("ctx-"))
                .build()
                .start();
        ManagedChannel channel2 = ManagedChannelBuilder
                .forAddress("localhost", server2.getPort())
                .usePlaintext(true)
                .intercept(new AmbientContextClientInterceptor("ctx-"))
                .build();
        GreeterGrpc.GreeterBlockingStub stub2 = GreeterGrpc.newBlockingStub(channel2);

        // Relay service
        GreeterGrpc.GreeterImplBase svc1 = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(stub2.sayHello(request));
                responseObserver.onCompleted();
            }
        };

        // Relay service plumbing
        Server server1 = ServerBuilder
                .forPort(0)
                .addService(svc2)
                .intercept(new AmbientContextServerInterceptor("ctx-"))
                .build()
                .start();
        ManagedChannel channel1 = ManagedChannelBuilder
                .forAddress("localhost", server1.getPort())
                .usePlaintext(true)
                .intercept(new AmbientContextClientInterceptor("ctx-"))
                .build();
        GreeterGrpc.GreeterBlockingStub stub1 = GreeterGrpc.newBlockingStub(channel2);

        // Test
        try {
            AmbientContext.initialize(Context.current()).run(() -> {
                AmbientContext.current().put(ctxKey, expectedCtxValue);
                stub1.sayHello(HelloRequest.newBuilder().setName("world").build());
            });

            assertThat(ctxValue.get()).isEqualTo(expectedCtxValue);
        } finally {
            channel2.shutdownNow();
            server2.shutdownNow();
            channel1.shutdownNow();
            server1.shutdownNow();
        }
    }

    @Test
    public void contextTransfersOneHopAsync() throws Exception {
        Metadata.Key<String> ctxKey = Metadata.Key.of("ctx-context-key", Metadata.ASCII_STRING_MARSHALLER);
        String expectedCtxValue = "context-value";
        AtomicReference<String> ctxValue = new AtomicReference<>();

        // Service
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                ctxValue.set(AmbientContext.current().get(ctxKey));
                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }
        };

        // Plumbing
        Server server = ServerBuilder
                .forPort(0)
                .addService(svc)
                .intercept(new AmbientContextServerInterceptor("ctx-"))
                .build()
                .start();
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", server.getPort())
                .usePlaintext(true)
                .intercept(new AmbientContextClientInterceptor("ctx-"))
                .build();
        GreeterGrpc.GreeterFutureStub stub = GreeterGrpc.newFutureStub(channel);

        // Test
        try {
            AmbientContext.initialize(Context.current()).run(() -> {
                AmbientContext.current().put(ctxKey, expectedCtxValue);
                ListenableFuture<HelloResponse> futureResponse = stub.sayHello(HelloRequest.newBuilder().setName("world").build());

                // Verify response callbacks still have context
                MoreFutures.onSuccess(futureResponse, response -> {
                    assertThat(AmbientContext.current().get(ctxKey)).isEqualTo(expectedCtxValue);
                }, Context.currentContextExecutor(Executors.newSingleThreadExecutor()));

                await().atMost(Duration.ONE_SECOND).until(futureResponse::isDone);
            });

            assertThat(ctxValue.get()).isEqualTo(expectedCtxValue);
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }
}
