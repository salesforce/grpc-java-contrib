/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import io.grpc.*;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.*;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@Ignore
public class ClientErrorIntegrationTest {
    private static Server server;
    private static ManagedChannel channel;
    private static ErrorCollectingGreeter svc;

    private static class ErrorCollectingGreeter extends RxGreeterGrpc.GreeterImplBase {
        private Throwable throwable;

        public Throwable getThrowable() {
            return throwable;
        }

        public void reset() {
            throwable = null;
        }

        @Override
        public Single<HelloResponse> sayHello(Single<HelloRequest> rxRequest) {
            rxRequest.subscribe(
                    System.out::println,
                    t -> {
                        System.out.println("Server " + t);
                        throwable = t;
                    }
            );

            return Single.just(HelloResponse.getDefaultInstance());
        }

        @Override
        public Flowable<HelloResponse> sayHelloRespStream(Single<HelloRequest> rxRequest) {
            rxRequest.subscribe(
                    System.out::println,
                    t -> {
                        System.out.println(t);
                        throwable = t;
                    }
            );

            return Flowable.just(HelloResponse.getDefaultInstance());
        }

        @Override
        public Single<HelloResponse> sayHelloReqStream(Flowable<HelloRequest> rxRequest) {
            rxRequest.subscribe(
                    System.out::println,
                    t -> {
                        System.out.println(t);
                        throwable = t;
                    }
            );

            return Single.just(HelloResponse.getDefaultInstance());
        }

        @Override
        public Flowable<HelloResponse> sayHelloBothStream(Flowable<HelloRequest> rxRequest) {
            rxRequest.subscribe(
                    System.out::println,
                    t -> {
                        System.out.println(t);
                        throwable = t;
                    }
            );

            return Flowable.just(HelloResponse.getDefaultInstance());
        }
    }

    @BeforeClass
    public static void setupServer() throws Exception {
        svc = new ErrorCollectingGreeter();
        server = ServerBuilder.forPort(0).addService(svc).build().start();
        channel = ManagedChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext(true).build();
    }

    @Before
    public void reset() {
        svc.reset();
    }

    @AfterClass
    public static void stopServer() {
        server.shutdown();
        channel.shutdown();
    }

    @Test
    public void oneToOne() {
        RxGreeterGrpc.RxGreeterStub stub = RxGreeterGrpc.newRxStub(channel);
        Single<HelloResponse> resp = stub.sayHello(Single.error(new Exception("Kaboom!")));
        TestObserver<HelloResponse> test = resp.test();

        resp.subscribe(
                System.out::println,
                t -> System.out.println("Client " + t)
        );

        test.awaitTerminalEvent(1, TimeUnit.SECONDS);
        assertThat(svc.getThrowable()).isNotNull();
    }

    @Test
    public void manyToOne() {
        RxGreeterGrpc.RxGreeterStub stub = RxGreeterGrpc.newRxStub(channel);
        Single<HelloResponse> resp = stub.sayHelloReqStream(Flowable.error(new Exception("Kaboom!")));
        TestObserver<HelloResponse> test = resp.test();

        resp.subscribe(
                System.out::println,
                t -> System.out.println("Client " + t)
        );

        test.awaitTerminalEvent(1, TimeUnit.SECONDS);
        assertThat(svc.getThrowable()).isNotNull();
    }
}
