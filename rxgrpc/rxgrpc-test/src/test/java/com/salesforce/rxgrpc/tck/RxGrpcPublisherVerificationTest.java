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

    /////////////////////


    @Override
    public void required_createPublisher1MustProduceAStreamOfExactly1Element() throws Throwable {
        super.required_createPublisher1MustProduceAStreamOfExactly1Element();
    }

    @Override
    public void required_createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
        super.required_createPublisher3MustProduceAStreamOfExactly3Elements();
    }

    @Override
    public void required_validate_maxElementsFromPublisher() throws Exception {
        super.required_validate_maxElementsFromPublisher();
    }

    @Override
    public void required_validate_boundedDepthOfOnNextAndRequestRecursion() throws Exception {
        super.required_validate_boundedDepthOfOnNextAndRequestRecursion();
    }

    @Override
    public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements() throws Throwable {
        super.required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements();
    }

    @Override
    public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable {
        super.required_spec102_maySignalLessThanRequestedAndTerminateSubscription();
    }

    @Override
    public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable {
        super.stochastic_spec103_mustSignalOnMethodsSequentially();
    }

    @Override
    public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
        super.required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates();
    }

    @Override
    public void optional_spec105_emptyStreamMustTerminateBySignallingOnComplete() throws Throwable {
        super.optional_spec105_emptyStreamMustTerminateBySignallingOnComplete();
    }

    @Override
    public void untested_spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled() throws Throwable {
        super.untested_spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled();
    }

    @Override
    public void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() throws Throwable {
        super.required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled();
    }

    @Override
    public void untested_spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() throws Throwable {
        super.untested_spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled();
    }

    @Override
    public void untested_spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() throws Throwable {
        super.untested_spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals();
    }

    @Override
    public void untested_spec109_subscribeShouldNotThrowNonFatalThrowable() throws Throwable {
        super.untested_spec109_subscribeShouldNotThrowNonFatalThrowable();
    }

    @Override
    public void required_spec109_subscribeThrowNPEOnNullSubscriber() throws Throwable {
        super.required_spec109_subscribeThrowNPEOnNullSubscriber();
    }

    @Override
    public void required_spec109_mustIssueOnSubscribeForNonNullSubscriber() throws Throwable {
        super.required_spec109_mustIssueOnSubscribeForNonNullSubscriber();
    }

    @Override
    public void untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice() throws Throwable {
        super.untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice();
    }

    @Override
    public void optional_spec111_maySupportMultiSubscribe() throws Throwable {
        super.optional_spec111_maySupportMultiSubscribe();
    }

    @Override
    public void optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals() throws Throwable {
        super.optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals();
    }

    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne() throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne();
    }

    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront() throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront();
    }

    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected() throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected();
    }

    @Override
    public void required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe() throws Throwable {
        super.required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe();
    }

    @Override
    public void required_spec303_mustNotAllowUnboundedRecursion() throws Throwable {
        super.required_spec303_mustNotAllowUnboundedRecursion();
    }

    @Override
    public void untested_spec304_requestShouldNotPerformHeavyComputations() throws Exception {
        super.untested_spec304_requestShouldNotPerformHeavyComputations();
    }

    @Override
    public void untested_spec305_cancelMustNotSynchronouslyPerformHeavyComputation() throws Exception {
        super.untested_spec305_cancelMustNotSynchronouslyPerformHeavyComputation();
    }

    @Override
    public void required_spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable {
        super.required_spec306_afterSubscriptionIsCancelledRequestMustBeNops();
    }

    @Override
    public void required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops() throws Throwable {
        super.required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops();
    }

    @Override
    public void required_spec309_requestZeroMustSignalIllegalArgumentException() throws Throwable {
        super.required_spec309_requestZeroMustSignalIllegalArgumentException();
    }

    @Override
    public void required_spec309_requestNegativeNumberMustSignalIllegalArgumentException() throws Throwable {
        super.required_spec309_requestNegativeNumberMustSignalIllegalArgumentException();
    }

    @Override
    public void required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling() throws Throwable {
        super.required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling();
    }

    @Override
    public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
        super.required_spec317_mustSupportAPendingElementCountUpToLongMaxValue();
    }

    @Override
    public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() throws Throwable {
        super.required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue();
    }

    @Override
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
        super.required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue();
    }
}
