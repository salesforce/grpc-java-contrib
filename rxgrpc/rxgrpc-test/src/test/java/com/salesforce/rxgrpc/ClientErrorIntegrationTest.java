/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import com.salesforce.grpc.contrib.LambdaStreamObserver;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.sql.Time;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("ALL")
@Ignore
public class ClientErrorIntegrationTest {
//    private static Server server;
//    private static ManagedChannel channel;
//    private static ErrorCollectingGreeter svc;
//
//    private static class ErrorCollectingGreeter extends RxGreeterGrpc.GreeterImplBase {
//        private Throwable throwable;
//
//        public Throwable getThrowable() {
//            return throwable;
//        }
//
//        public void reset() {
//            throwable = null;
//        }
//
//        @Override
//        public Single<HelloResponse> sayHello(Single<HelloRequest> rxRequest) {
//            rxRequest.subscribe(
//                    System.out::println,
//                    t -> {
//                        System.out.println("Server " + t);
//                        throwable = t;
//                    }
//            );
//
//            return Single.just(HelloResponse.getDefaultInstance());
//        }
//
//        @Override
//        public Flowable<HelloResponse> sayHelloRespStream(Single<HelloRequest> rxRequest) {
//            rxRequest.subscribe(
//                    r -> {
//                        System.out.println("Server " + r);
//                    },
//                    t -> {
//                        System.out.println(t);
//                        throwable = t;
//                    }
//            );
//
//            return Flowable.just(HelloResponse.getDefaultInstance());
//        }
//
//        @Override
//        public Single<HelloResponse> sayHelloReqStream(Flowable<HelloRequest> rxRequest) {
//            rxRequest.subscribe(
//                    r -> {
//                        System.out.println("Server " + r);
//                    },
//                    t -> {
//                        System.out.println(t);
//                        throwable = t;
//                    }
//            );
//
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return Single.just(HelloResponse.getDefaultInstance());
//        }
//
//        @Override
//        public Flowable<HelloResponse> sayHelloBothStream(Flowable<HelloRequest> rxRequest) {
//            rxRequest.subscribe(
//                    System.out::println,
//                    t -> {
//                        System.out.println(t);
//                        throwable = t;
//                    }
//            );
//
//            return Flowable.just(HelloResponse.getDefaultInstance());
//        }
//    }
//
//    @BeforeClass
//    public static void setupServer() throws Exception {
//        svc = new ErrorCollectingGreeter();
//        server = ServerBuilder.forPort(0).addService(svc).build().start();
//        channel = ManagedChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext(true).build();
//    }
//
//    @Before
//    public void reset() {
//        svc.reset();
//    }
//
//    @AfterClass
//    public static void stopServer() {
//        server.shutdown();
//        channel.shutdown();
//    }
//
//    @Test
//    public void oneToOne() {
//        RxGreeterGrpc.RxGreeterStub stub = RxGreeterGrpc.newRxStub(channel);
//        Single<HelloResponse> resp = stub.sayHello(Single.error(new Exception("Kaboom!")));
//        TestObserver<HelloResponse> test = resp.test();
//
//        resp.subscribe(
//                System.out::println,
//                t -> System.out.println("Client " + t)
//        );
//
//        test.awaitTerminalEvent(3, TimeUnit.SECONDS);
//        assertThat(svc.getThrowable()).isNotNull();
//    }
//
//    @Test
//    public void manyToOne() throws InterruptedException {
//        RxGreeterGrpc.RxGreeterStub stub = RxGreeterGrpc.newRxStub(channel);
//
//        Flowable<HelloRequest> rxRequest = Flowable.generate(emitter -> {
//
//            emitter.onNext(HelloRequest.getDefaultInstance());
//            emitter.onNext(HelloRequest.getDefaultInstance());
//            emitter.onNext(HelloRequest.getDefaultInstance());
//
//            //            emitter.onError(new Exception("Kaboom!"));
//        });
//
//        Single<HelloResponse> resp = stub.sayHelloReqStream(rxRequest);
//
//        TestObserver<HelloResponse> test = resp.test();
//
////        resp.subscribe(
////                System.out::println,
////                t -> System.out.println("Client " + t)
////        );
//
//        test.awaitTerminalEvent(3, TimeUnit.SECONDS);
//        assertThat(svc.getThrowable()).isNotNull();
//    }


    @Test
    public void rawGrpcTest() throws InterruptedException, IOException {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public StreamObserver<HelloRequest> sayHelloReqStream(StreamObserver<HelloResponse> responseObserver) {
                return new LambdaStreamObserver<HelloRequest>(
                        value -> {
                            System.out.println("Server: " + value);
                        },
                        throwable -> {
                            System.out.println("Server error: " + throwable);
                            responseObserver.onNext(HelloResponse.newBuilder().setMessage("XXX").build());
                            responseObserver.onCompleted();
                        },
                        () -> {
                            System.out.println("Server: complete");
                            responseObserver.onNext(HelloResponse.newBuilder().setMessage("YYY").build());
                            responseObserver.onCompleted();
                        }
                );
            }
        };

        Server server = ServerBuilder.forPort(9999).addService(svc).build().start();
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext(true).build();

//        ConnectableFlowable<HelloRequest> rxRequest = Flowable.<HelloRequest>generate(emitter -> {
//            emitter.onNext(HelloRequest.newBuilder().setName("AAA").build());
//            emitter.onNext(HelloRequest.newBuilder().setName("BBB").build());
//            emitter.onNext(HelloRequest.newBuilder().setName("CCC").build());
//
//            //            emitter.onError(new Exception("Kaboom!"));
//        }).publish();

//        Flowable<HelloRequest> rxRequest = Flowable.just(
//                HelloRequest.newBuilder().setName("AAA").build(),
//                HelloRequest.newBuilder().setName("BBB").build(),
//                HelloRequest.newBuilder().setName("CCC").build()
//        );

        Flowable<HelloRequest> rxRequest = Flowable.<HelloRequest>create(emitter -> {
            emitter.onNext(HelloRequest.newBuilder().setName("AAA").build());
            Thread.sleep(1000);
            emitter.onNext(HelloRequest.newBuilder().setName("BBB").build());
            Thread.sleep(1000);
            emitter.onNext(HelloRequest.newBuilder().setName("CCC").build());
            Thread.sleep(1000);
            emitter.onComplete();

//            emitter.onError(new Exception("Kaboom!"));
        }, BackpressureStrategy.BUFFER);

        RxGreeterGrpc.RxGreeterStub stub = RxGreeterGrpc.newRxStub(channel);
        Single<HelloResponse> resp = stub.sayHelloReqStream(rxRequest);

//        rxRequest.connect();

        resp.subscribe(System.out::println, System.out::println);

        Thread.sleep(3000);
    }
}
