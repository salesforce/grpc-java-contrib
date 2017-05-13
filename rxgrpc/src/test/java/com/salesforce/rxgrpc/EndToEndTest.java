/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class EndToEndTest {
    private static Server server;
    private static ManagedChannel channel;

    @BeforeClass
    public static void setupServer() throws Exception {
        GreeterGrpc.GreeterImplBase svc = new RxGreeterImplBase() {

            @Override
            public Single<HelloResponse> sayHello(Single<HelloRequest> rxRequest) {
                return rxRequest.map(protoRequest -> greet("Hello", protoRequest));
            }

            @Override
            public Observable<HelloResponse> sayHelloRespStream(Single<HelloRequest> rxRequest) {
                return rxRequest.flatMapObservable(protoRequest -> Observable.just(
                        greet("Hello", protoRequest),
                        greet("Hi", protoRequest),
                        greet("Greetings", protoRequest)));
            }

            @Override
            public Single<HelloResponse> sayHelloReqStream(Observable<HelloRequest> rxRequest) {
                return rxRequest
                        .map(HelloRequest::getName)
                        .toList()
                        .map(names -> greet("Hello", String.join(" and ", names)));
            }

            @Override
            public Observable<HelloResponse> sayHelloBothStream(Observable<HelloRequest> rxRequest) {
                return rxRequest
                        .map(HelloRequest::getName)
                        .buffer(2)
                        .map(names -> greet("Hello", String.join(" and ", names)));
            }

            private HelloResponse greet(String greeting, HelloRequest request) {
                return greet(greeting, request.getName());
            }

            private HelloResponse greet(String greeting, String name) {
                return HelloResponse.newBuilder().setMessage(greeting + " " + name).build();
            }
        };

        server = InProcessServerBuilder.forName("e2e").addService(svc).build().start();
        channel = InProcessChannelBuilder.forName("e2e").usePlaintext(true).build();
    }

    @AfterClass
    public static void stopServer() {
        server.shutdown();
        channel.shutdown();
    }

    @Test
    public void oneToOne() throws IOException {
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

        String hello = stub.sayHello(HelloRequest.newBuilder().setName("rxjava").build()).getMessage();

        assertThat(hello).isEqualTo("Hello rxjava");
    }

    @Test
    public void oneToMany() throws IOException {
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

        Iterator<HelloResponse> iterator = stub.sayHelloRespStream(HelloRequest.newBuilder().setName("rxjava").build());
        List<String> responses = new ArrayList<>();
        iterator.forEachRemaining(helloResponse -> responses.add(helloResponse.getMessage()));

        assertThat(responses).contains("Hello rxjava", "Hi rxjava", "Greetings rxjava");
    }

    @Test
    public void manyToOne() throws Exception {
        GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
        AtomicBoolean called = new AtomicBoolean(false);

        StreamObserver<HelloRequest> requestObserver = stub.sayHelloReqStream(new StreamObserver<HelloResponse>() {
            HelloResponse response;
            @Override
            public void onNext(HelloResponse value) {
                if (response == null) {
                    response = value;
                } else {
                    fail();
                }
                called.set(true);
            }

            @Override
            public void onError(Throwable t) {
                fail(t.toString());
            }

            @Override
            public void onCompleted() {
                assertThat(response.getMessage()).isEqualTo("Hello a and b and c");
            }
        });

        requestObserver.onNext(HelloRequest.newBuilder().setName("a").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("b").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("c").build());
        requestObserver.onCompleted();

        Thread.sleep(1000);

        assertThat(called.get()).isTrue();
    }

    @Test
    public void manyToMany() throws Exception {
        GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
        AtomicBoolean called = new AtomicBoolean(false);

        StreamObserver<HelloRequest> requestObserver = stub.sayHelloBothStream(new StreamObserver<HelloResponse>() {
            List<HelloResponse> responses = new ArrayList<>();
            @Override
            public void onNext(HelloResponse value) {
                responses.add(value);
            }

            @Override
            public void onError(Throwable t) {
                fail(t.toString());
            }

            @Override
            public void onCompleted() {
                assertThat(responses.get(0).getMessage()).isEqualTo("Hello a and b");
                assertThat(responses.get(1).getMessage()).isEqualTo("Hello c and d");
                called.set(true);
            }
        });

        requestObserver.onNext(HelloRequest.newBuilder().setName("a").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("b").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("c").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("d").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("e").build());
        requestObserver.onCompleted();

        Thread.sleep(1000);

        assertThat(called.get()).isTrue();
    }
}
