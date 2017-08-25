/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.util.concurrent.*;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ALL")
public class MoreFuturesTest {
    @Test
    public void addCallbackSuccess() {
        AtomicBoolean called = new AtomicBoolean(false);
        final Object value = new Object();

        SettableFuture<Object> future = SettableFuture.create();
        MoreFutures.onSuccess(
                future,
                o -> { assertEquals(value, o); called.set(true); },
                MoreExecutors.directExecutor());

        assertFalse(called.get());
        future.set(value);
        assertTrue(called.get());
    }

    @Test
    public void addCallbackFailure() {
        AtomicBoolean called = new AtomicBoolean(false);
        final Exception ex = new Exception();

        SettableFuture<Object> future = SettableFuture.create();
        MoreFutures.onFailure(
                future,
                e -> { assertEquals(ex, e); called.set(true); },
                MoreExecutors.directExecutor());

        assertFalse(called.get());
        future.setException(ex);
        assertTrue(called.get());
    }

    @Test
    public void getDoneUncheckedReturnsExpectedResultForCompletedFuture() {
        final String result = UUID.randomUUID().toString();

        ListenableFuture<String> future = Futures.immediateFuture(result);

        assertThat(MoreFutures.getDoneUnchecked(future)).isEqualTo(result);
    }

    @Test
    public void getDoneUncheckedThrowsForIncompleteFuture() {
        ListeningExecutorService executorService =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

        CountDownLatch latch = new CountDownLatch(1);

        ListenableFuture<String> future = executorService.submit(() -> {
            latch.await(1, TimeUnit.MINUTES);

            return null;
        });

        assertThatThrownBy(() -> MoreFutures.getDoneUnchecked(future))
                .isInstanceOf(IllegalStateException.class);

        latch.countDown();
    }

    @Test
    public void getDoneUncheckedThrowsForCancelledFuture() {
        ListenableFuture<String> future = Futures.immediateCancelledFuture();

        assertThatThrownBy(() -> MoreFutures.getDoneUnchecked(future))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getDoneUncheckedThrowsForExplodingFuture() {
        final RuntimeException exception = new RuntimeException(UUID.randomUUID().toString());

        ListeningExecutorService executorService =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

        ListenableFuture<String> future = executorService.submit(() -> {
            throw exception;
        });

        await().atMost(1, TimeUnit.MINUTES).until(future::isDone);

        assertThatThrownBy(() -> MoreFutures.getDoneUnchecked(future))
                .isInstanceOf(UncheckedExecutionException.class)
                .hasCause(exception);
    }

    @Test
    public void onGrpcFailureHandlesStatusException() {
        AtomicBoolean called = new AtomicBoolean(false);

        Metadata trailers = new Metadata();
        Throwable exception = Status.NOT_FOUND.asException(trailers);

        SettableFuture<String> future = SettableFuture.create();
        MoreFutures.onGrpcFailure(future, (status, metadata) -> {
            assertThat(status.getCode()).isEqualTo(Status.Code.NOT_FOUND);
            assertThat(metadata).isEqualTo(trailers);
            called.set(true);
        }, MoreExecutors.directExecutor());

        assertThat(called.get()).isFalse();
        future.setException(exception);
        assertThat(called.get()).isTrue();
    }

    @Test
    public void onGrpcFailureHandlesStatusRuntimeException() {
        AtomicBoolean called = new AtomicBoolean(false);

        Metadata trailers = new Metadata();
        Throwable exception = Status.NOT_FOUND.asRuntimeException(trailers);

        SettableFuture<String> future = SettableFuture.create();
        MoreFutures.onGrpcFailure(future, (status, metadata) -> {
            assertThat(status.getCode()).isEqualTo(Status.Code.NOT_FOUND);
            assertThat(metadata).isEqualTo(trailers);
            called.set(true);
        }, MoreExecutors.directExecutor());

        assertThat(called.get()).isFalse();
        future.setException(exception);
        assertThat(called.get()).isTrue();
    }

    @Test
    public void onGrpcFailureIgnoresNonGrpcException() {
        AtomicBoolean called = new AtomicBoolean(false);

        Throwable exception = new Exception("Kaboom!");

        SettableFuture<String> future = SettableFuture.create();
        MoreFutures.onGrpcFailure(future, (status, metadata) -> {
            called.set(true);
        }, MoreExecutors.directExecutor());

        assertThat(called.get()).isFalse();
        future.setException(exception);
        assertThat(called.get()).isFalse();
    }

    @Test
    public void onGrpcFailureHandlesSpecificStatusException() {
        AtomicBoolean called = new AtomicBoolean(false);

        Metadata trailers = new Metadata();
        Throwable exception = Status.NOT_FOUND.asException(trailers);

        SettableFuture<String> future = SettableFuture.create();
        MoreFutures.onGrpcFailure(future, Status.Code.NOT_FOUND, (status, metadata) -> {
            assertThat(status.getCode()).isEqualTo(Status.Code.NOT_FOUND);
            assertThat(metadata).isEqualTo(trailers);
            called.set(true);
        }, MoreExecutors.directExecutor());

        assertThat(called.get()).isFalse();
        future.setException(exception);
        assertThat(called.get()).isTrue();
    }

    @Test
    public void onGrpcFailureHandlesIgnoresSpecificStatusException() {
        AtomicBoolean called = new AtomicBoolean(false);

        Metadata trailers = new Metadata();
        Throwable exception = Status.NOT_FOUND.asException(trailers);

        SettableFuture<String> future = SettableFuture.create();
        MoreFutures.onGrpcFailure(future, Status.Code.UNIMPLEMENTED, (status, metadata) -> {
            called.set(true);
        }, MoreExecutors.directExecutor());

        assertThat(called.get()).isFalse();
        future.setException(exception);
        assertThat(called.get()).isFalse();
    }
}
