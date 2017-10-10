/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import com.google.common.util.concurrent.ListenableFuture;
import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import com.salesforce.grpc.contrib.MoreFutures;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

public class AmbientContextTransferTest {
    @Rule
    public GrpcServerRule serverRule1 = new GrpcServerRule();
    @Rule public GrpcServerRule serverRule2 = new GrpcServerRule();

    @Before
    public void setUp() throws Exception {
        // Reset the gRPC context between test executions
        Context.ROOT.attach();
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
        serverRule1.getServiceRegistry().addService(ServerInterceptors
                .intercept(svc, new AmbientContextServerInterceptor("ctx-")));

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule1.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            AmbientContext.current().put(ctxKey, expectedCtxValue);
            stub.sayHello(HelloRequest.newBuilder().setName("world").build());
        });

        assertThat(ctxValue.get()).isEqualTo(expectedCtxValue);
    }

    @Test
    public void multiValueContextTransfers() throws Exception {
        Metadata.Key<String> ctxKey = Metadata.Key.of("ctx-context-key", Metadata.ASCII_STRING_MARSHALLER);
        String expectedCtxValue1 = "context-value1";
        String expectedCtxValue2 = "context-value2";
        String expectedCtxValue3 = "context-value3";
        AtomicReference<Iterable<String>> ctxValue = new AtomicReference<>();

        // Service
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                ctxValue.set(AmbientContext.current().getAll(ctxKey));
                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }
        };

        // Plumbing
        serverRule1.getServiceRegistry().addService(ServerInterceptors
                .intercept(svc, new AmbientContextServerInterceptor("ctx-")));

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule1.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            AmbientContext.current().put(ctxKey, expectedCtxValue1);
            AmbientContext.current().put(ctxKey, expectedCtxValue2);
            AmbientContext.current().put(ctxKey, expectedCtxValue3);
            stub.sayHello(HelloRequest.newBuilder().setName("world").build());
        });

        assertThat(ctxValue.get()).containsExactlyInAnyOrder(expectedCtxValue1, expectedCtxValue2, expectedCtxValue3);
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
        serverRule2.getServiceRegistry().addService(ServerInterceptors
                .intercept(svc2, new AmbientContextServerInterceptor("ctx-")));
        GreeterGrpc.GreeterBlockingStub stub2 = GreeterGrpc
                .newBlockingStub(serverRule2.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Relay service
        GreeterGrpc.GreeterImplBase svc1 = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(stub2.sayHello(request));
                responseObserver.onCompleted();
            }
        };

        // Relay service plumbing
        serverRule1.getServiceRegistry().addService(ServerInterceptors
                .intercept(svc1, new AmbientContextServerInterceptor("ctx-")));
        GreeterGrpc.GreeterBlockingStub stub1 = GreeterGrpc
                .newBlockingStub(serverRule1.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            AmbientContext.current().put(ctxKey, expectedCtxValue);
            stub1.sayHello(HelloRequest.newBuilder().setName("world").build());
        });

        assertThat(ctxValue.get()).isEqualTo(expectedCtxValue);
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
        serverRule1.getServiceRegistry().addService(ServerInterceptors
                .intercept(svc, new AmbientContextServerInterceptor("ctx-")));
        GreeterGrpc.GreeterFutureStub stub = GreeterGrpc
                .newFutureStub(serverRule1.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            AmbientContext.current().put(ctxKey, expectedCtxValue);
            ListenableFuture<HelloResponse> futureResponse = stub.sayHello(HelloRequest.newBuilder().setName("world").build());

            // Verify response callbacks still have context
            MoreFutures.onSuccess(
                    futureResponse,
                    response -> assertThat(AmbientContext.current().get(ctxKey)).isEqualTo(expectedCtxValue),
                    Context.currentContextExecutor(Executors.newSingleThreadExecutor()));

            await().atMost(Duration.ONE_SECOND).until(futureResponse::isDone);
        });

        assertThat(ctxValue.get()).isEqualTo(expectedCtxValue);
    }

    @Test
    public void multipleContextTransfersOneHopSync() throws Exception {
        Metadata.Key<String> ctxKey = Metadata.Key.of("ctx-context-key", Metadata.ASCII_STRING_MARSHALLER);
        Metadata.Key<String> l5dKey = Metadata.Key.of("l5d-context-key", Metadata.ASCII_STRING_MARSHALLER);
        String expectedCtxValue = "context-value";
        AtomicReference<String> ctxValue = new AtomicReference<>();
        AtomicReference<String> l5dValue = new AtomicReference<>();

        // Service
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                ctxValue.set(AmbientContext.current().get(ctxKey));
                l5dValue.set(AmbientContext.current().get(l5dKey));
                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }
        };

        // Plumbing
        serverRule1.getServiceRegistry().addService(ServerInterceptors.intercept(svc,
                new AmbientContextServerInterceptor("ctx-"),
                new AmbientContextServerInterceptor("l5d-")));

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule1.getChannel())
                .withInterceptors(
                        new AmbientContextClientInterceptor("ctx-"),
                        new AmbientContextClientInterceptor("l5d-"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            AmbientContext.current().put(ctxKey, expectedCtxValue);
            AmbientContext.current().put(l5dKey, expectedCtxValue);
            stub.sayHello(HelloRequest.newBuilder().setName("world").build());
        });

        assertThat(ctxValue.get()).isEqualTo(expectedCtxValue);
        assertThat(l5dValue.get()).isEqualTo(expectedCtxValue);
    }
}
