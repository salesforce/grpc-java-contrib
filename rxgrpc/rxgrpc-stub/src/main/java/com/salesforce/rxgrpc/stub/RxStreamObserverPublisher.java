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
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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

    public RxStreamObserverPublisher(CallStreamObserver callStreamObserver) {
        Preconditions.checkNotNull(callStreamObserver);
        this.callStreamObserver = callStreamObserver;
        callStreamObserver.disableAutoInboundFlowControl();
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Preconditions.checkNotNull(subscriber);
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                callStreamObserver.request((int) l);
            }

            @Override
            public void cancel() {
                callStreamObserver.onError(new StatusException(Status.CANCELLED));
            }
        });
        this.subscriber = subscriber;
    }

    @Override
    public void onNext(T value) {
        Preconditions.checkState(subscriber != null, "subscribe() has not been called yet");
        subscriber.onNext(Preconditions.checkNotNull(value));
    }

    @Override
    public void onError(Throwable t) {
        Preconditions.checkState(subscriber != null, "subscribe() has not been called yet");
        subscriber.onError(Preconditions.checkNotNull(t));
    }

    @Override
    public void onCompleted() {
        Preconditions.checkState(subscriber != null, "subscribe() has not been called yet");
        subscriber.onComplete();
    }
}
