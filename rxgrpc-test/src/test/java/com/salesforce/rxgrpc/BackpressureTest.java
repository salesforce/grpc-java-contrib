/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import com.google.common.collect.Lists;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("Duplicates")
public class BackpressureTest {
    private static Server server;
    private static ManagedChannel channel;

    @BeforeClass
    public static void setupServer() throws Exception {
        GreeterGrpc.GreeterImplBase svc = new RxGreeterGrpc.GreeterImplBase() {
            @Override
            public Observable<HelloResponse> sayHelloBothStream(Observable<HelloRequest> request) {
                return request
                        .observeOn(Schedulers.computation(), false, 10)
                        .map(HelloRequest::getName)
                        .doOnNext(city -> System.out.println("--> Receiving " + city))
                        .map(name -> "Hello " + name)
                        .doOnNext(greeting -> System.out.println("<-- Sending " + greeting))
                        .map(greeting -> HelloResponse.newBuilder().setMessage(greeting).build());
            }
        };

        server = ServerBuilder.forPort(0).addService(svc).build().start();
        System.out.println("Server port " + server.getPort());
        channel = ManagedChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext(true).build();
    }

    @AfterClass
    public static void stopServer() {
        server.shutdown();
        channel.shutdown();
    }

    @Test
    public void backPressureShouldSlowProductionDown() throws Exception {
        RxGreeterGrpc.RxGreeterStub stub = RxGreeterGrpc.newRxStub(channel);
        Object lock = new Object();

//        Observable<HelloRequest> requests = Observable.fromArray(
//                "Shanghai", "Karachi", "Beijing", "Delhi", "Lagos", "Tianjin", "Istanbul", "Tokyo", "Guangzhou",
//                "Mumbai", "Moscow", "São Paulo", "Shenzhen", "Jakarta", "Lahore", "Seoul", "Wuhan", "Kinshasa",
//                "Cairo", "Mexico City")
//        Observable<HelloRequest> requests = Observable.range(1, 1000)
//                .map(i -> Integer.toString(i))
//                .doOnNext(city -> System.out.println("--> Sending " + city))
//                .map(city -> HelloRequest.newBuilder().setName(city).build());

        Flowable<HelloRequest> requests = Flowable.<Integer, Integer>generate(() -> 0, (i, emitter) -> {
                    if (i > 100) {
                        emitter.onComplete();
                        return i;
                    } else {
                        emitter.onNext(i);
                        return ++i;
                    }
                })
                .map(i -> Integer.toString(i))
                .doOnNext(city -> System.out.println("--> Sending " + city))
                .map(city -> HelloRequest.newBuilder().setName(city).build());

        stub.sayHelloBothStream(requests)
                .map(HelloResponse::getMessage)
                .doOnNext(greeting -> System.out.println("<-- Receiving " + greeting))
                .doOnTerminate(() -> { synchronized (lock) {lock.notify();}})
                .forEach(System.out::println);

        synchronized (lock) {
            lock.wait();
        }
    }

    @Test
    public void backPressureShouldSlowProductionDown2() throws Exception {
        GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
        Object lock = new Object();

        StreamObserver<HelloResponse> responseObserver = new ClientResponseObserver<HelloRequest, HelloResponse>() {
            ClientCallStreamObserver ccso;

            @Override
            public void beforeStart(ClientCallStreamObserver requestStream) {
                requestStream.disableAutoInboundFlowControl();
                ccso = requestStream;
            }

            @Override
            public void onNext(HelloResponse value) {
                String greeting = value.getMessage();
                System.out.println("<-- Receiving " + greeting);
                System.out.println(greeting);
                ccso.request(1);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Completed");
                synchronized (lock) {lock.notify();}
            }
        };

        CallStreamObserver<HelloRequest> requestObserver = (CallStreamObserver<HelloRequest>) stub.sayHelloBothStream(responseObserver);
//        List<String> cities = Lists.asList("Shanghai", new String[] {"Karachi", "Beijing", "Delhi", "Lagos", "Tianjin", "Istanbul", "Tokyo", "Guangzhou",
//                "Mumbai", "Moscow", "São Paulo", "Shenzhen", "Jakarta", "Lahore", "Seoul", "Wuhan", "Kinshasa",
//                "Cairo", "Mexico City"});

        List<String> cities = IntStream.range(0, 500).boxed().map(i -> Integer.toString(i)).collect(Collectors.toList());


        cities.forEach(city -> {
            while(!requestObserver.isReady()) {}
            System.out.println("--> Sending " + city);
            requestObserver.onNext(HelloRequest.newBuilder().setName(city).build());
        });
        requestObserver.onCompleted();


        synchronized (lock) {
            lock.wait();
        }
    }
}
