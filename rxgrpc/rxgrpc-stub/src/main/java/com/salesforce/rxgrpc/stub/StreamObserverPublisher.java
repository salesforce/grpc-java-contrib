/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * StreamObserverPublisher.
 * @param <T>
 */
public class StreamObserverPublisher<T> implements Publisher<T>, StreamObserver<T> {
    private CallStreamObserver callStreamObserver;
    private Subscriber<? super T> subscriber;

    public StreamObserverPublisher(CallStreamObserver callStreamObserver) {
        this.callStreamObserver = callStreamObserver;
        callStreamObserver.disableAutoInboundFlowControl();
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                System.out.println("Request " + l);
                callStreamObserver.request((int) l);
            }

            @Override
            public void cancel() {

            }
        });
    }

    @Override
    public void onNext(T value) {
        subscriber.onNext(value);
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
        subscriber.onError(t);
    }

    @Override
    public void onCompleted() {
        subscriber.onComplete();
    }
}
