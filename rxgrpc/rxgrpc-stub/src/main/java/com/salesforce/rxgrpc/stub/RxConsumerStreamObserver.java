/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public class RxConsumerStreamObserver<TRequest, TResponse> implements ClientResponseObserver<TRequest, TResponse> {
    private StreamObserverPublisher<TResponse> publisher;
    private Flowable<TResponse> rxConsumer;


    @Override
    public void beforeStart(ClientCallStreamObserver<TRequest> requestStream) {
        publisher = new StreamObserverPublisher<>(requestStream);
        rxConsumer = Flowable.unsafeCreate(publisher).observeOn(Schedulers.single());
    }

    @Override
    public void onNext(TResponse value) {
        publisher.onNext(value);
    }

    @Override
    public void onError(Throwable throwable) {
        publisher.onError(throwable);
    }

    @Override
    public void onCompleted() {
        publisher.onCompleted();
    }

    public Flowable<TResponse> getRxConsumer() {
        return rxConsumer;
    }
}