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

/**
 * RxConsumerStreamObserver configures client-side manual flow control for the consuming end of a message stream.
 *
 * @param <TRequest>
 * @param <TResponse>
 */
public class RxConsumerStreamObserver<TRequest, TResponse> implements ClientResponseObserver<TRequest, TResponse> {
    private RxStreamObserverPublisher<TResponse> publisher;
    private Flowable<TResponse> rxConsumer;

    public Flowable<TResponse> getRxConsumer() {
        return rxConsumer;
    }

    @Override
    public void beforeStart(ClientCallStreamObserver<TRequest> requestStream) {
        publisher = new RxStreamObserverPublisher<>(requestStream);
        rxConsumer = Flowable.unsafeCreate(publisher)
                .observeOn(Schedulers.from(RxExecutor.getSerializingExecutor()));
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
}