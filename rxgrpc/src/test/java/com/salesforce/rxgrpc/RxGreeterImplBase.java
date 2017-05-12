/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import io.grpc.stub.StreamObserver;
import io.reactivex.*;

public abstract class RxGreeterImplBase extends GreeterGrpc.GreeterImplBase {
    public abstract Single<HelloResponse> sayHello(Single<HelloRequest> request);

    public abstract Observable<HelloResponse> sayHelloRespStream(Single<HelloRequest> request);

    public abstract Single<HelloResponse> sayHelloReqStream(Observable<HelloRequest> request);

    public abstract Observable<HelloResponse> sayHelloBothStream(Observable<HelloRequest> request);

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        try {
            Single<HelloRequest> rxRequest = Single.just(request);

            Single<HelloResponse> rxResponse = sayHello(rxRequest);
            rxResponse.subscribe(
                    responseObserver::onNext,
                    responseObserver::onError);
            responseObserver.onCompleted();
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void sayHelloRespStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        try {
            Single<HelloRequest> rxRequest = Single.just(request);

            Observable<HelloResponse> rxResponse = sayHelloRespStream(rxRequest);
            rxResponse.subscribe(
                    responseObserver::onNext,
                    responseObserver::onError,
                    responseObserver::onCompleted);
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
            responseObserver.onCompleted();
        }
    }

    @Override
    public StreamObserver<HelloRequest> sayHelloReqStream(StreamObserver<HelloResponse> responseObserver) {
        ObservableBridgeEmitter<HelloRequest> requestEmitter = new ObservableBridgeEmitter<>();

        try {
            Single<HelloResponse> rxResponse = sayHelloReqStream(Observable.create(requestEmitter));
            rxResponse.subscribe(value -> {
                        responseObserver.onNext(value);
                        responseObserver.onCompleted();
                    },
                    throwable -> {
                        responseObserver.onError(throwable);
                        responseObserver.onCompleted();
                    });
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
            responseObserver.onCompleted();
        }

        return new StreamObserver<HelloRequest>() {
            @Override
            public void onNext(HelloRequest value) {
                requestEmitter.onNext(value);
            }

            @Override
            public void onError(Throwable t) {
                requestEmitter.onError(t);
            }

            @Override
            public void onCompleted() {
                requestEmitter.onComplete();
            }
        };
    }

    @Override
    public StreamObserver<HelloRequest> sayHelloBothStream(StreamObserver<HelloResponse> responseObserver) {
        ObservableBridgeEmitter<HelloRequest> requestEmitter = new ObservableBridgeEmitter<>();

        try {
            Observable<HelloResponse> rxResponse = sayHelloBothStream(Observable.create(requestEmitter));
            rxResponse.subscribe(responseObserver::onNext, responseObserver::onError, responseObserver::onCompleted);
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
            responseObserver.onCompleted();
        }

        return new StreamObserver<HelloRequest>() {
            @Override
            public void onNext(HelloRequest value) {
                requestEmitter.onNext(value);
            }

            @Override
            public void onError(Throwable t) {
                requestEmitter.onError(t);
            }

            @Override
            public void onCompleted() {
                requestEmitter.onComplete();
            }
        };
    }

    private class ObservableBridgeEmitter<T> implements ObservableOnSubscribe<T>, Emitter<T> {
        private ObservableEmitter<T> observableEmitter;

        @Override
        public void subscribe(ObservableEmitter<T> ObservableEmitter) throws Exception {
            this.observableEmitter = ObservableEmitter;
        }

        @Override
        public void onNext(T t) {
            observableEmitter.onNext(t);
        }

        @Override
        public void onError(Throwable throwable) {
            observableEmitter.onError(throwable);
        }

        @Override
        public void onComplete() {
            observableEmitter.onComplete();
        }
    }
}
