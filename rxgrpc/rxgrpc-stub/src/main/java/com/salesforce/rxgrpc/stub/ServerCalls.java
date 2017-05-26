/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import io.grpc.stub.StreamObserver;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.function.Function;

/**
 * ServerCalls.
 */
public final class ServerCalls {
    private ServerCalls() {

    }

    public static <TRequest, TResponse> void oneToOne(
            TRequest request, StreamObserver<TResponse> responseObserver,
            Function<Single<TRequest>, Single<TResponse>> delegate) {
        try {
            Single<TRequest> rxRequest = Single.just(request);

            Single<TResponse> rxResponse = delegate.apply(rxRequest);
            rxResponse.subscribe(
                    responseObserver::onNext,
                    responseObserver::onError);
            responseObserver.onCompleted();
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }
    }

    public static <TRequest, TResponse> void oneToMany(
            TRequest request, StreamObserver<TResponse> responseObserver,
            Function<Single<TRequest>, Observable<TResponse>> delegate) {
        try {
            Single<TRequest> rxRequest = Single.just(request);

            Observable<TResponse> rxResponse = delegate.apply(rxRequest);
            rxResponse.subscribe(
                    responseObserver::onNext,
                    responseObserver::onError,
                    responseObserver::onCompleted);
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }
    }

    public static <TRequest, TResponse> StreamObserver<TRequest> manyToOne(
            StreamObserver<TResponse> responseObserver,
            Function<Observable<TRequest>, Single<TResponse>> delegate) {
        ObservableBridgeEmitter<TRequest> requestEmitter = new ObservableBridgeEmitter<>();

        try {
            Single<TResponse> rxResponse = delegate.apply(Observable.create(requestEmitter));
            rxResponse.subscribe(value -> {
                    responseObserver.onNext(value);
                    responseObserver.onCompleted();
                },
                responseObserver::onError
            );
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }

        return new RxStreamObserver<>(requestEmitter::onNext, requestEmitter::onError, requestEmitter::onComplete);
    }

    public static <TRequest, TResponse> StreamObserver<TRequest> manyToMany(
            StreamObserver<TResponse> responseObserver,
            Function<Observable<TRequest>, Observable<TResponse>> delegate) {
        ObservableBridgeEmitter<TRequest> requestEmitter = new ObservableBridgeEmitter<>();

        try {
            Observable<TResponse> rxResponse = delegate.apply(Observable.create(requestEmitter));
            rxResponse.subscribe(responseObserver::onNext, responseObserver::onError, responseObserver::onCompleted);
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }

        return new RxStreamObserver<>(requestEmitter::onNext, requestEmitter::onError, requestEmitter::onComplete);
    }
}
