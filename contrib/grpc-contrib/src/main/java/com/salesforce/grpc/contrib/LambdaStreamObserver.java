/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.util.concurrent.Runnables;
import io.grpc.stub.StreamObserver;

import java.util.function.Consumer;

/**
 * LambdaStreamObserver allows the construction of a {@link StreamObserver} instance from a triplet of lambda functions.
 *
 * @param <T>
 */
public class LambdaStreamObserver<T> implements StreamObserver<T> {
    private Consumer<T> onNext;
    private Consumer<Throwable> onError;
    private Runnable onCompleted;

    public LambdaStreamObserver(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
        this.onNext = onNext;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    public LambdaStreamObserver(Consumer<T> onNext, Consumer<Throwable> onError) {
        this(
            onNext,
            onError,
            Runnables.doNothing()
        );
    }

    public LambdaStreamObserver(Consumer<T> onNext) {
        this(
            onNext,
            t -> {
                throw new OnErrorNotImplementedException(t);
            },
            Runnables.doNothing()
        );
    }

    /**
     * OnErrorNotImplementedException is used to signal that LambdaStreamObserver's onError handler was not implemented.
     */
    public static final class OnErrorNotImplementedException extends RuntimeException {
        private OnErrorNotImplementedException(Throwable cause) {
            super(cause.getMessage(), cause);
        }
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