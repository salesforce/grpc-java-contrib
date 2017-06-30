/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import com.google.common.base.Preconditions;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.reactivex.subscribers.SafeSubscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;

/**
 * RxStreamObserverPublisher bridges the manual flow control idioms of gRPC and RxJava. This class takes
 * messages off of a {@link StreamObserver} and feeds them into a {@link Publisher} while respecting backpressure. This
 * class is the inverse of {@link RxFlowableBackpressureOnReadyHandler}.
 * <p>
 * When a {@link Publisher} is subscribed to by a {@link Subscriber}, the {@code Publisher} hands the {@code Subscriber}
 * a {@link Subscription}. When the {@code Subscriber} wants more messages from the {@code Publisher}, the
 * {@code Subscriber} calls {@link Subscription#request(long)}.
 * <p>
 * gRPC also uses the {@link CallStreamObserver#request(int)} idiom to request more messages from the stream.
 * <p>
 * To bridge the two idioms: this class implements a {@code Publisher} which delegates calls to {@code request()} to
 * a {@link CallStreamObserver} set in the constructor. When a message is generated as a response, the message is
 * delegated in the reverse so the {@code Publisher} can announce it to RxJava.
 *
 * @param <T>
 */
public class RxStreamObserverPublisher<T> implements Publisher<T>, StreamObserver<T> {
    private CallStreamObserver callStreamObserver;
    private Subscriber<? super T> subscriber;

    // A gRPC server can sometimes send messages before subscribe() has been called and the consumer may not have
    // finished setting up the consumer pipeline. Use a countdown latch to prevent messages from processing before
    // subscribe() has been called.
    private CountDownLatch subscribed = new CountDownLatch(1);

    public RxStreamObserverPublisher(CallStreamObserver callStreamObserver) {
        Preconditions.checkNotNull(callStreamObserver);
        this.callStreamObserver = callStreamObserver;
        callStreamObserver.disableAutoInboundFlowControl();
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Preconditions.checkNotNull(subscriber);
        subscriber = new SafeSubscriber<>(subscriber);
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                callStreamObserver.request((int) l);
            }

            @Override
            public void cancel() {
                callStreamObserver.onError(Status.CANCELLED.asRuntimeException());
            }
        });
        this.subscriber = subscriber;

        subscribed.countDown();
    }

    @Override
    public void onNext(T value) {
        try {
            subscribed.await();
        } catch (InterruptedException e) {

        }
        subscriber.onNext(Preconditions.checkNotNull(value));
    }

    @Override
    public void onError(Throwable t) {
        try {
            subscribed.await();
        } catch (InterruptedException e) {

        }

        if (t instanceof StatusException &&
                ((StatusException) t).getStatus().getCode() == Status.Code.CANCELLED) {
            subscriber.onComplete();
        } else if (t instanceof StatusRuntimeException &&
                ((StatusRuntimeException) t).getStatus().getCode() == Status.Code.CANCELLED) {
            subscriber.onComplete();
        } else {
            subscriber.onError(Preconditions.checkNotNull(t));
        }
    }

    @Override
    public void onCompleted() {
        try {
            subscribed.await();
        } catch (InterruptedException e) {

        }
        subscriber.onComplete();
    }
}
