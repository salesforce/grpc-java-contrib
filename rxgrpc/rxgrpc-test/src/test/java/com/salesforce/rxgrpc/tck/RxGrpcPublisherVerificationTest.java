/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.tck;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Publisher tests from the Reactive Streams Technology Compatibility Kit.
 * https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck
 */
public class RxGrpcPublisherVerificationTest extends PublisherVerification<Message> {
    public static final long DEFAULT_TIMEOUT_MILLIS = 100L;
    public static final long PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS = 500L;

    public RxGrpcPublisherVerificationTest() {
        super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS, DEFAULT_TIMEOUT_MILLIS), PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS);
    }

    private static Server server;
    private static ManagedChannel channel;

    @BeforeClass
    public static void setup() throws IOException {
        server = InProcessServerBuilder.forName("RxGrpcPublisherVerificationTest").addService(new TckService()).build().start();
        channel = InProcessChannelBuilder.forName("RxGrpcPublisherVerificationTest").usePlaintext(true).build();
    }

    @AfterClass
    public static void tearDown() {
        server.shutdownNow();
        channel.shutdownNow();
    }

    @Override
    public Publisher<Message> createPublisher(long elements) {
        RxTckGrpc.RxTckStub stub = RxTckGrpc.newRxStub(channel);
        Flowable<Message> request = Flowable.range(0, (int)elements).map(this::toMessage);
        return stub.manyToMany(request);
    }

    @Override
    public Publisher<Message> createFailedPublisher() {
        RxTckGrpc.RxTckStub stub = RxTckGrpc.newRxStub(channel);
        Flowable<Message> request = Flowable.just(toMessage(TckService.KABOOM));
        return stub.manyToMany(request);
//        return null;
    }

    private Message toMessage(int i) {
        return Message.newBuilder().setNumber(i).build();
    }

    @Override @Test
    public void optional_spec104_mustSignalOnErrorWhenFails() throws Throwable {
        super.optional_spec104_mustSignalOnErrorWhenFails();
    }
}
