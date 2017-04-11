/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.util.concurrent.*;

import javax.annotation.Nonnull;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

public final class MoreFutures {
    private MoreFutures() {
        // prevent instantiation
    }

    /**
     * Registers separate success and failure callbacks to be run when the {@code Future}'s
     * computation is {@linkplain java.util.concurrent.Future#isDone() complete} or, if the
     * computation is already complete, immediately.
     *
     * @see Futures#addCallback(ListenableFuture, FutureCallback, Executor)
     * @param future The future attach the callback to.
     * @param success A {@link Consumer} to execute when the future succeeds.
     * @param failure A {@link Consumer} to execute when the future fails.
     * @param executor The executor to run {@code callback} when the future completes.
     */
    public static <V> void addCallback(
            @Nonnull final ListenableFuture<V> future,
            @Nonnull final Consumer<V> success,
            @Nonnull final Consumer<Throwable> failure,
            @Nonnull final Executor executor) {
        checkNotNull(future, "future");
        checkNotNull(success, "success");
        checkNotNull(failure, "failure");
        checkNotNull(executor, "executor");

        FutureCallback<V> futureCallback = new FutureCallback<V>() {
            @Override
            public void onSuccess(V result) {
                success.accept(result);
            }

            @Override
            public void onFailure(Throwable t) {
                failure.accept(t);
            }
        };
        Futures.addCallback(future, futureCallback, executor);
    }

    /**
     * Registers a success callback to be run when the {@code Future}'s
     * computation is {@linkplain java.util.concurrent.Future#isDone() complete} or, if the
     * computation is already complete, immediately.
     *
     * @see Futures#addCallback(ListenableFuture, FutureCallback, Executor)
     * @param future The future attach the callback to.
     * @param success A {@link Consumer} to execute when the future succeeds.
     * @param executor The executor to run {@code callback} when the future completes.
     */
    public static <V> void onSuccess(@Nonnull final ListenableFuture<V> future,
                                     @Nonnull final Consumer<V> success,
                                     @Nonnull final Executor executor) {
        checkNotNull(future, "future");
        checkNotNull(success, "success");
        checkNotNull(executor, "executor");

        addCallback(future, success, throwable -> {}, executor);
    }

    /**
     * Registers a failure callback to be run when the {@code Future}'s
     * computation is {@linkplain java.util.concurrent.Future#isDone() complete} or, if the
     * computation is already complete, immediately.
     *
     * @see Futures#addCallback(ListenableFuture, FutureCallback, Executor)
     * @param future The future attach the callback to.
     * @param failure A {@link Consumer} to execute when the future fails.
     * @param executor The executor to run {@code callback} when the future completes.
     */
    public static <V> void onFailure(@Nonnull final ListenableFuture<V> future,
                                     @Nonnull final Consumer<Throwable> failure,
                                     @Nonnull final Executor executor) {
        checkNotNull(future, "future");
        checkNotNull(failure, "failure");
        checkNotNull(executor, "executor");

        addCallback(future, v -> {}, failure, executor);
    }

    /**
     * Returns the result of the input {@code Future}, which must have already completed.
     *
     * <p>The benefits of this method are twofold. First, the name "getDone" suggests to readers that
     * the {@code Future} is already done. Second, if buggy code calls {@code getDone} on a {@code
     * Future} that is still pending, the program will throw instead of block.
     *
     * <p>If you are looking for a method to determine whether a given {@code Future} is done, use the
     * instance method {@link Future#isDone()}.
     *
     * @throws ExecutionException if the {@code Future} failed with an exception
     * @throws CancellationException if the {@code Future} was cancelled
     * @throws IllegalStateException if the {@code Future} is not done
     * @since Guava 20.0
     */
    public static <V> V getDone(Future<V> future) throws ExecutionException {
    /*
     * We throw IllegalStateException, since the call could succeed later. Perhaps we "should" throw
     * IllegalArgumentException, since the call could succeed with a different argument. Those
     * exceptions' docs suggest that either is acceptable. Google's Java Practices page recommends
     * IllegalArgumentException here, in part to keep its recommendation simple: Static methods
     * should throw IllegalStateException only when they use static state.
     *
     *
     * Why do we deviate here? The answer: We want for fluentFuture.getDone() to throw the same
     * exception as Futures.getDone(fluentFuture).
     */
        checkState(future.isDone(), "Future was expected to be done: %s", future);
        return getUninterruptibly(future);
    }

    /**
     * Converts a Guava {@link ListenableFuture} into a JDK {@link CompletableFuture}, preserving value, exception,
     * and cancellation propagation.
     *
     * <p>The resulting {@link CompletableFuture} acts on the same {@link Executor} as the provided {@link ListenableFuture}.
     * @param listenableFuture A {@link ListenableFuture} to adapt.
     * @return A new {@link CompletableFuture}.
     */
    public static <V> CompletableFuture<V> toCompletableFuture(@Nonnull final ListenableFuture<V> listenableFuture) {
        checkNotNull(listenableFuture, "listenableFuture");

        // Setup backward cancellation propagation CF -> LF
        final CompletableFuture<V> completableFuture = new CompletableFuture<V>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return super.cancel(mayInterruptIfRunning) && listenableFuture.cancel(mayInterruptIfRunning);
            }

            // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html
            // CompletableFuture.cancel(bool) is the same as CompleteExceptionally(new CancellationException())
            @Override
            public boolean completeExceptionally(Throwable ex) {
                if (ex instanceof CancellationException) {
                    listenableFuture.cancel(true);
                }
                return super.completeExceptionally(ex);
            }
        };

        // Setup forward event propagation LF -> CF
        Runnable callbackRunnable = () -> {
            try {
                final V value = listenableFuture.get();
                completableFuture.complete(value);
            } catch (CancellationException ex) { // the ListenableFuture was cancelled
                completableFuture.cancel(true);
            } catch (ExecutionException ex) { // the ListenableFuture failed with a exception
                completableFuture.completeExceptionally(ex.getCause());
            } catch (RuntimeException | Error ex) { // the ListenableFuture failed with a REALLY BAD exception
                completableFuture.completeExceptionally(ex);
            } catch (InterruptedException ex) {
                completableFuture.completeExceptionally(ex); // Won't happen since get() only called after completion
            }
        };
        listenableFuture.addListener(callbackRunnable, MoreExecutors.directExecutor());

        return completableFuture;
    }

    /**
     * Converts a JDK {@link CompletableFuture} into a Guava {@link ListenableFuture}, preserving value, exception,
     * and cancellation propagation.
     *
     * <p>The resulting {@link ListenableFuture} acts on the same {@link Executor} as the provided {@link CompletableFuture}.
     * @param completableFuture A {@link CompletableFuture} to adapt.
     * @return A new {@link ListenableFuture}.
     */
    public static <V> ListenableFuture<V> fromCompletableFuture(@Nonnull final CompletableFuture<V> completableFuture) {
        checkNotNull(completableFuture, "completableFuture");

        // Setup backward cancellation propagation LF -> CF
        Settable<V> listenableFuture = new Settable<V>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return super.cancel(mayInterruptIfRunning) && completableFuture.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean setException(Throwable ex) {
                if (ex instanceof CancellationException) {
                    completableFuture.cancel(true);
                }
                return super.setException(ex);
            }
        };

        // Setup forward event propagation CF -> LF
        completableFuture.whenComplete((value, ex) -> {
            if (value != null) {
                listenableFuture.set(value);
            } else {
                if (ex instanceof CancellationException) {
                    listenableFuture.cancel(true);
                } else {
                    listenableFuture.setException(ex);
                }
            }
        });

        return listenableFuture;
    }

    /**
     * A private helper class to assist fromCompletableFuture()
     */
    private static class Settable<V> extends AbstractFuture<V> {
        @Override
        public boolean set(V value) {
            return super.set(value);
        }

        @Override
        public boolean setException(Throwable throwable) {
            return super.setException(throwable);
        }
    }
}
