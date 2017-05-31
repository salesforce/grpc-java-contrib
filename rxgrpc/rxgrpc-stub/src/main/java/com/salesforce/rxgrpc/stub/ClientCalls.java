/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import com.google.common.util.concurrent.Runnables;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * ClientCalls.
 */
public final class ClientCalls {
    private ClientCalls() {

    }

    public static <TRequest, TResponse> Single<TResponse> oneToOne(
            Single<TRequest> rxRequest,
            BiConsumer<TRequest, StreamObserver<TResponse>> delegate) {
        try {
            return Single.create(emitter -> rxRequest.subscribe(
                request -> delegate.accept(request, new RxStreamObserver<TResponse>(
                    emitter::onSuccess,
                    emitter::onError,
                    Runnables.doNothing()
                )),
                emitter::onError
            ));
        } catch (Throwable throwable) {
            return Single.error(throwable);
        }
    }

    public static <TRequest, TResponse> Flowable<TResponse> oneToMany(
            Single<TRequest> rxRequest,
            BiConsumer<TRequest, StreamObserver<TResponse>> delegate) {
        try {
            RxConsumerStreamObserver<TRequest, TResponse> consumerStreamObserver = new RxConsumerStreamObserver<>();
            rxRequest.subscribe(request -> delegate.accept(request, consumerStreamObserver));
            return consumerStreamObserver.getRxConsumer();
        } catch (Throwable throwable) {
            return Flowable.error(throwable);
        }
    }

    public static <TRequest, TResponse> Single<TResponse> manyToOne(
            Flowable<TRequest> rxRequest,
            Function<StreamObserver<TResponse>, StreamObserver<TRequest>> delegate) {
        try {
            return Single.create(emitter -> delegate.apply(new RxProducerStreamObserver<>(
                rxRequest,
                emitter::onSuccess,
                emitter::onError,
                Runnables.doNothing()))
            );
        } catch (Throwable throwable) {
            return Single.error(throwable);
        }
    }

    public static <TRequest, TResponse> Flowable<TResponse> manyToMany(
            Flowable<TRequest> rxRequest,
            Function<StreamObserver<TResponse>, StreamObserver<TRequest>> delegate) {
        try {
            RxProducerConsumerStreamObserver<TRequest, TResponse> consumerStreamObserver = new RxProducerConsumerStreamObserver<>(rxRequest);
            delegate.apply(consumerStreamObserver);
            return consumerStreamObserver.getRxConsumer();
        } catch (Throwable throwable) {
            return Flowable.error(throwable);
        }
    }
}
