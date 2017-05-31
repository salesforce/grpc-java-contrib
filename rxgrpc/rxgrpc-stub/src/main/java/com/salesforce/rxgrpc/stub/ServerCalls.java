/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

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
            Function<Single<TRequest>, Flowable<TResponse>> delegate) {
        try {
            Single<TRequest> rxRequest = Single.just(request);

            Flowable<TResponse> rxResponse = delegate.apply(rxRequest);
            rxResponse.subscribe(new RxFlowableBackpressureOnReadyHandler<>(
                    (CallStreamObserver<TResponse>)responseObserver));
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }
    }

    public static <TRequest, TResponse> StreamObserver<TRequest> manyToOne(
            StreamObserver<TResponse> responseObserver,
            Function<Flowable<TRequest>, Single<TResponse>> delegate) {
        RxStreamObserverPublisher<TRequest> streamObserverPublisher =
                new RxStreamObserverPublisher<>((CallStreamObserver) responseObserver);

        try {
            Single<TResponse> rxResponse = delegate.apply(
                    Flowable.unsafeCreate(streamObserverPublisher).observeOn(Schedulers.single()));
            rxResponse.subscribe(
                value -> {
                    responseObserver.onNext(value);
                    responseObserver.onCompleted();
                },
                responseObserver::onError
            );
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }

        return new RxStreamObserver<>(
                streamObserverPublisher::onNext,
                streamObserverPublisher::onError,
                streamObserverPublisher::onCompleted);
    }

    public static <TRequest, TResponse> StreamObserver<TRequest> manyToMany(
            StreamObserver<TResponse> responseObserver,
            Function<Flowable<TRequest>, Flowable<TResponse>> delegate) {
        RxStreamObserverPublisher<TRequest> streamObserverPublisher =
                new RxStreamObserverPublisher<>((CallStreamObserver) responseObserver);

        try {
            Flowable<TResponse> rxResponse = delegate.apply(
                    Flowable.unsafeCreate(streamObserverPublisher).observeOn(Schedulers.single()));
            rxResponse.subscribe(new RxFlowableBackpressureOnReadyHandler<>(
                    (CallStreamObserver<TResponse>)responseObserver));
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
        }

        return new RxStreamObserver<>(
                streamObserverPublisher::onNext,
                streamObserverPublisher::onError,
                streamObserverPublisher::onCompleted);
    }
}
