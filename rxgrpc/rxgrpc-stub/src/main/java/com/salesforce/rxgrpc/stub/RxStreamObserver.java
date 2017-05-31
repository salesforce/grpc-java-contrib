/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import io.grpc.stub.StreamObserver;

import java.util.function.Consumer;

/**
 * RxStreamObserver allows the construction of a {@link StreamObserver} instance from a triplet of lambda functions.
 *
 * @param <T>
 */
public class RxStreamObserver<T> implements StreamObserver<T> {
    private Consumer<T> onNext;
    private Consumer<Throwable> onError;
    private Runnable onCompleted;

    public RxStreamObserver(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
        this.onNext = onNext;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public void onNext(T value) {
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