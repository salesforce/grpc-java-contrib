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
import org.testng.annotations.*;

import java.io.IOException;
import java.util.UUID;

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

    @BeforeTest
    public static void setup() throws IOException {
        UUID name = UUID.randomUUID();
        server = InProcessServerBuilder.forName(name.toString()).addService(new TckService()).build().start();
        channel = InProcessChannelBuilder.forName(name.toString()).usePlaintext(true).build();
    }

    @AfterTest
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


    @Test(enabled = false)
    @Override
    public void optional_spec104_mustSignalOnErrorWhenFails() throws Throwable {
        super.optional_spec104_mustSignalOnErrorWhenFails();
    }

    @Test(enabled = false)
    @Override
    public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe() throws Throwable {
        super.required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe();
    }

    @Test(enabled = false)
    @Override
    public void required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber() throws Throwable {
        super.required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber();
    }

    @Test(enabled = false, description = "gRPC streams are hot, so multiple subscribes don't make sense and aren't supported")
    @Override
    public void optional_spec111_maySupportMultiSubscribe() throws Throwable {
        super.optional_spec111_maySupportMultiSubscribe();
    }

    @Test(enabled = false, description = "gRPC streams are hot, so multiple subscribes don't make sense and aren't supported")
    @Override
    public void optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals() throws Throwable {
        super.optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals();
    }

    @Test(enabled = false, description = "gRPC streams are hot, so multiple subscribes don't make sense and aren't supported")
    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne() throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne();
    }

    @Test(enabled = false, description = "gRPC streams are hot, so multiple subscribes don't make sense and aren't supported")
    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront() throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront();
    }

    @Test(enabled = false, description = "gRPC streams are hot, so multiple subscribes don't make sense and aren't supported")
    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected() throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected();
    }

    /////////////////////


    @Override
    public void required_createPublisher1MustProduceAStreamOfExactly1Element() throws Throwable {
        super.required_createPublisher1MustProduceAStreamOfExactly1Element();
    }

    @Override
    public void required_createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
        super.required_createPublisher3MustProduceAStreamOfExactly3Elements();
    }
}
