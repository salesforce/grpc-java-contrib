/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import com.google.protobuf.Empty;
import com.salesforce.servicelibs.NumberProto;
import com.salesforce.servicelibs.RxNumbersGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ALL")
public class CancellationPropagationIntegrationTest {
    private static Server server;
    private static ManagedChannel channel;
    private static TestService svc = new TestService();

    private static class TestService extends RxNumbersGrpc.NumbersImplBase {
        private int lastNumberProduced = Integer.MIN_VALUE;
        private boolean wasCanceled = false;
        private boolean explicitCancel = false;

        public void reset() {
            lastNumberProduced = Integer.MIN_VALUE;
            wasCanceled = false;
            explicitCancel = false;
        }

        public int getLastNumberProduced() {
            return lastNumberProduced;
        }

        public boolean wasCanceled() {
            return wasCanceled;
        }

        public void setExplicitCancel(boolean explicitCancel) {
            this.explicitCancel = explicitCancel;
        }

        @Override
        public Flowable<NumberProto.Number> responsePressure(Single<Empty> request) {
            // Produce a very long sequence
            return Flowable.fromIterable(new Sequence(10000))
                    .doOnNext(i -> lastNumberProduced = i)
                    .map(CancellationPropagationIntegrationTest::protoNum)
                    .doOnCancel(() -> {
                        wasCanceled = true;
                        System.out.println("Server canceled");
                    });
        }

        @Override
        public Single<NumberProto.Number> requestPressure(Flowable<NumberProto.Number> request) {
            if (explicitCancel) {
                // Process some of a very long sequence
                return request.map(req -> req.getNumber(0))
                        .doOnNext(System.out::println)
                        .take(10)
                        .last(-1)
                        .map(CancellationPropagationIntegrationTest::protoNum);
            } else {
                // Process a very long sequence
                return null;
            }
        }
    }

    @BeforeClass
    public static void setupServer() throws Exception {
        server = InProcessServerBuilder.forName("e2e").addService(svc).build().start();
        channel = InProcessChannelBuilder.forName("e2e").usePlaintext(true).build();
    }

    @Before
    public void resetServerStats() {
        svc.reset();
    }

    @AfterClass
    public static void stopServer() {
        server.shutdown();
        channel.shutdown();
    }

    @Test
    public void clientCanCancelServerStreamExplicitly() throws InterruptedException {
        RxNumbersGrpc.RxNumbersStub stub = RxNumbersGrpc.newRxStub(channel);
        TestSubscriber<NumberProto.Number> subscription = stub
                .responsePressure(Single.just(Empty.getDefaultInstance()))
                .doOnNext(number -> System.out.println(number.getNumber(0)))
                .doOnError(throwable -> System.out.println(throwable.getMessage()))
                .doOnComplete(() -> System.out.println("Completed"))
                .doOnCancel(() -> System.out.println("Client canceled"))
                .test();

        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        subscription.dispose();

        subscription.awaitTerminalEvent(1, TimeUnit.SECONDS);
        // Cancellation may or may not deliver the last generated message due to race conditions
        assertThat(Math.abs(subscription.valueCount() - svc.getLastNumberProduced())).isLessThanOrEqualTo(1);
        subscription.assertTerminated();
        assertThat(svc.wasCanceled()).isTrue();
    }

    @Test
    public void clientCanCancelServerStreamImplicitly() throws InterruptedException {
        RxNumbersGrpc.RxNumbersStub stub = RxNumbersGrpc.newRxStub(channel);
        TestSubscriber<NumberProto.Number> subscription = stub
                .responsePressure(Single.just(Empty.getDefaultInstance()))
                .doOnNext(number -> System.out.println(number.getNumber(0)))
                .doOnError(throwable -> System.out.println(throwable.getMessage()))
                .doOnComplete(() -> System.out.println("Completed"))
                .doOnCancel(() -> System.out.println("Client canceled"))
                .take(10)
                .test();

        // Consume some work
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        subscription.dispose();

        subscription.awaitTerminalEvent(1, TimeUnit.SECONDS);
        subscription.assertValueCount(10);
        subscription.assertTerminated();
        assertThat(svc.wasCanceled()).isTrue();
    }

    @Test
    public void serverCanCancelClientStreamExplicitly() {
        RxNumbersGrpc.RxNumbersStub stub = RxNumbersGrpc.newRxStub(channel);

        svc.setExplicitCancel(true);

        AtomicBoolean wasCanceled = new AtomicBoolean(false);
        Flowable<NumberProto.Number> request = Flowable.fromIterable(new Sequence(10000))
                .map(CancellationPropagationIntegrationTest::protoNum)
                .doOnCancel(() -> {
                    wasCanceled.set(true);
                    System.out.println("Client canceled");
                });

        TestObserver<NumberProto.Number> observer = stub
                .requestPressure(request)
                .doOnSuccess(number -> System.out.println(number.getNumber(0)))
                .doOnError(throwable -> System.out.println(throwable.getMessage()))
                .test();

        observer.awaitTerminalEvent();
        observer.assertError(StatusRuntimeException.class);
        observer.assertTerminated();
        assertThat(wasCanceled.get()).isTrue();
    }

    @Test
    public void serverCanCancelClientStreamImplicitly() {

    }

    private static NumberProto.Number protoNum(int i) {
        Integer[] ints = {i};
        return NumberProto.Number.newBuilder().addAllNumber(Arrays.asList(ints)).build();
    }
}
