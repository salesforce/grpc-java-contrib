/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * FutureChain provides a fluent interface for chaining {@link ListenableFuture}s together.
 * @param <T>
 */
public final class FutureChain<T> {
    /**
     * Starts the construction of a {@link ListenableFuture} chain.
     * @param startFuture The first {@link ListenableFuture} in the chain.
     * @param executor The initial {@link Executor} to use for each step of the chain.
     */
    public static <T> FutureChain<T> startWith(ListenableFuture<T> startFuture, Executor executor) {
        return new FutureChain<>(startFuture, executor);
    }

    private final ListenableFuture<T> future;
    private final Executor executor;

    private FutureChain(ListenableFuture<T> future, Executor executor) {
        this.future = future;
        this.executor = executor;
    }

    /**
     * Switches the {@link Executor} used by all subsequent chained calls.
     * @param newExecutor The executor to use.
     */
    public FutureChain<T> switchExecutor(Executor newExecutor) {
        return new FutureChain<>(future, newExecutor);
    }

    /**
     * @see Futures#transform(ListenableFuture, Function, Executor)
     */
    public <U> FutureChain<U> transform(Function<? super T, ? extends U> function) {
        return new FutureChain<>(Futures.transform(future, function, executor), executor);
    }

    /**
     * @see Futures#transformAsync(ListenableFuture, AsyncFunction, Executor)
     */
    public <U> FutureChain<U> transformAsync(AsyncFunction<? super T, ? extends U> function) {
        return new FutureChain<>(Futures.transformAsync(future, function, executor), executor);
    }

    /**
     * @see Futures#catching(ListenableFuture, Class, Function, Executor)
     */
    public <E extends Throwable> FutureChain<T> catching(Class<E> exceptionType, Function<? super E, ? extends T> fallback) {
        return new FutureChain<>(Futures.catching(future, exceptionType, fallback, executor), executor);
    }

    /**
     * @see Futures#catchingAsync(ListenableFuture, Class, AsyncFunction, Executor)
     */
    public <E extends Throwable> FutureChain<T> catchingAsync(Class<E> exceptionType, AsyncFunction<? super E, ? extends T> fallback) {
        return new FutureChain<>(Futures.catchingAsync(future, exceptionType, fallback, executor), executor);
    }

    /**
     * @see Futures#addCallback(ListenableFuture, FutureCallback, Executor)
     */
    public FutureChain<T> addCallback(FutureCallback<? super T> callback) {
        Futures.addCallback(future, callback, executor);
        return this;
    }

    /**
     * @see MoreFutures#addCallback(ListenableFuture, Consumer, Consumer, Executor)
     */
    public FutureChain<T> addCallback(@Nonnull final Consumer<T> success, @Nonnull final Consumer<Throwable> failure) {
        MoreFutures.addCallback(future, success, failure, executor);
        return this;
    }

    /**
     * @see MoreFutures#onSuccess(ListenableFuture, Consumer, Executor)
     */
    public FutureChain<T> onSuccess(@Nonnull final Consumer<T> success) {
        MoreFutures.onSuccess(future, success, executor);
        return this;
    }

    /**
     * @see MoreFutures#onFailure(ListenableFuture, Consumer, Executor)
     */
    public FutureChain<T> onFailure(@Nonnull final Consumer<Throwable> failure) {
        MoreFutures.onFailure(future, failure, executor);
        return this;
    }

    /**
     * Returns the compiled {@link ListenableFuture} chain.
     */
    public ListenableFuture<T> compile() {
        return future;
    }
}
