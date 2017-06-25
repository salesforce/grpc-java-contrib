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

import java.util.concurrent.ArrayBlockingQueue;

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
    private static final int MESSAGE_BUFFER_SIZE = 16;
    private CallStreamObserver callStreamObserver;
    private Subscriber<? super T> subscriber;

    // A gRPC server can sometimes send an error before subscribe() has been called and the consumer may not have
    // finished setting up the consumer pipeline. Buffer up to one error and 16 messages from the server in case this
    // happens.
    private Throwable errorBuffer;
    private ArrayBlockingQueue<T> messageBuffer = new ArrayBlockingQueue<>(MESSAGE_BUFFER_SIZE);
    private boolean completedBuffer;

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

        // Process any buffered responses
        if (!messageBuffer.isEmpty()) {
            for (T msg : messageBuffer) {
                onNext(msg);
            }
        }
        if (errorBuffer != null) {
            onError(errorBuffer);
        }
        if (completedBuffer) {
            onCompleted();
        }
    }

    @Override
    public void onNext(T value) {
        if (subscriber == null) {
            Preconditions.checkState(messageBuffer.offer(value), "more than 16 calls to onNext() before subscribe()");
        } else {
            subscriber.onNext(Preconditions.checkNotNull(value));
        }
    }

    @Override
    public void onError(Throwable t) {
        if (subscriber == null) {
            Preconditions.checkState(errorBuffer == null, "onError() called twice before subscribe()");
            errorBuffer = t;
        } else {
            subscriber.onError(Preconditions.checkNotNull(t));
        }
    }

    @Override
    public void onCompleted() {
        if (subscriber == null) {
            completedBuffer = true;
        } else {
            subscriber.onComplete();
        }
    }
}
