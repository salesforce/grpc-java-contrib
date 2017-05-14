/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import com.google.common.util.concurrent.Runnables;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.reactivex.*;

import java.util.function.Consumer;
import java.util.function.Function;

public final class GreeterGrpcRx {
    public static RxGreeterStub newRxStub(Channel channel) {
        return new RxGreeterStub(channel);
    }

    public static final class RxGreeterStub {
        private GreeterGrpc.GreeterStub delegateStub;

        private RxGreeterStub(Channel channel) {
            delegateStub = GreeterGrpc.newStub(channel);
        }

        public Single<HelloResponse> sayHello(Single<HelloRequest> rxRequest) {
            return Single.create(emitter -> rxRequest.subscribe(
                    request -> delegateStub.sayHello(request, new RxStreamObserver<HelloResponse>(emitter)),
                    emitter::onError
            ));
        }
    }

    public static abstract class GreeterImplBase extends GreeterGrpc.GreeterImplBase {
        public abstract Single<HelloResponse> sayHello(Single<HelloRequest> request);

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            oneToOne(request, responseObserver, this::sayHello);
        }

        public abstract Observable<HelloResponse> sayHelloRespStream(Single<HelloRequest> request);

        @Override
        public void sayHelloRespStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            oneToMany(request, responseObserver, this::sayHelloRespStream);
        }

        public abstract Single<HelloResponse> sayHelloReqStream(Observable<HelloRequest> request);

        @Override
        public StreamObserver<HelloRequest> sayHelloReqStream(StreamObserver<HelloResponse> responseObserver) {
            return manyToOne(responseObserver, this::sayHelloReqStream);
        }

        public abstract Observable<HelloResponse> sayHelloBothStream(Observable<HelloRequest> request);

        @Override
        public StreamObserver<HelloRequest> sayHelloBothStream(StreamObserver<HelloResponse> responseObserver) {
            return manyToMany(responseObserver, this::sayHelloBothStream);
        }
    }

    private static <TRequest, TResponse> void oneToOne(
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
            responseObserver.onCompleted();
        }
    }

    private static <TRequest, TResponse> void oneToMany(
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
            responseObserver.onCompleted();
        }
    }

    private static <TRequest, TResponse> StreamObserver<TRequest> manyToOne(
            StreamObserver<TResponse> responseObserver,
            Function<Observable<TRequest>, Single<TResponse>> delegate) {
        ObservableBridgeEmitter<TRequest> requestEmitter = new ObservableBridgeEmitter<>();

        try {
            Single<TResponse> rxResponse = delegate.apply(Observable.create(requestEmitter));
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

        return new RxStreamObserver<>(requestEmitter);
    }

    private static <TRequest, TResponse> StreamObserver<TRequest> manyToMany(
            StreamObserver<TResponse> responseObserver,
            Function<Observable<TRequest>, Observable<TResponse>> delegate) {
        ObservableBridgeEmitter<TRequest> requestEmitter = new ObservableBridgeEmitter<>();

        try {
            Observable<TResponse> rxResponse = delegate.apply(Observable.create(requestEmitter));
            rxResponse.subscribe(responseObserver::onNext, responseObserver::onError, responseObserver::onCompleted);
        } catch (Throwable throwable) {
            responseObserver.onError(throwable);
            responseObserver.onCompleted();
        }

        return new RxStreamObserver<>(requestEmitter);
    }

    private static class RxStreamObserver<V> implements StreamObserver<V> {
        private Consumer<V> onNext;
        private Consumer<Throwable> onError;
        private Runnable onCompleted;

        private RxStreamObserver(Emitter<V> requestEmitter) {
            this(requestEmitter::onNext, requestEmitter::onError, requestEmitter::onComplete);
        }

        private RxStreamObserver(SingleEmitter<V> requestEmitter) {
            this(requestEmitter::onSuccess, requestEmitter::onError, Runnables.doNothing());
        }

        private RxStreamObserver(Consumer<V> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
            this.onNext = onNext;
            this.onError = onError;
            this.onCompleted = onCompleted;
        }

        @Override
        public void onNext(V value) {
            onNext.accept(value);
        }

        @Override
        public void onError(Throwable t) {
            onError.accept(t);
        }

        @Override
        public void onCompleted() {
            onCompleted.run();
        }
    }

    private static class ObservableBridgeEmitter<T> implements ObservableOnSubscribe<T>, Emitter<T> {
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
