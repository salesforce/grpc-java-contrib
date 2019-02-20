/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.util.concurrent.*;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class ToCompletableFutureTest {
    @Test
    public void incompleteShouldReportIncomplete() {
        SettableFuture<String> lf = SettableFuture.create();
        CompletableFuture<String> cf = MoreFutures.toCompletableFuture(lf);

        assertThat(cf).isNotCancelled();
        assertThat(cf).isNotCompleted();
        assertThat(cf).isNotCompletedExceptionally();
        assertThat(cf).isNotDone();
    }

    @Test
    public void forwardCompletionShouldWork() throws Exception {
        final String value = UUID.randomUUID().toString();

        SettableFuture<String> lf = SettableFuture.create();
        CompletableFuture<String> cf = MoreFutures.toCompletableFuture(lf);

        lf.set(value);

        assertThat(cf).isDone();
        assertThat(cf).isNotCancelled();
        assertThat(cf).isNotCompletedExceptionally();

        assertThat(cf).isCompletedWithValue(value);
    }

    @Test
    public void forwardExceptionShouldWork() throws Exception {
        SettableFuture<String> lf = SettableFuture.create();
        CompletableFuture<String> cf = MoreFutures.toCompletableFuture(lf);

        Exception intentionalException = new IntentionalException();
        lf.setException(intentionalException);

        assertThat(cf).isDone();
        assertThat(cf).isNotCancelled();
        assertThat(cf).isCompletedExceptionally();

        assertThat(cf).hasFailedWithThrowableThat()
                .isSameAs(intentionalException);
    }

    @Test
    public void forwardExceptionShouldPropagate() throws Exception {
        final String value = UUID.randomUUID().toString();

        SettableFuture<String> lf = SettableFuture.create();
        CompletableFuture<String> cf = MoreFutures.toCompletableFuture(lf);

        Exception intentionalException = new IntentionalException();
        final AtomicReference<Throwable> foundException = new AtomicReference<>();

        cf = cf.exceptionally(ex -> {
            foundException.set(ex);
            return value;
        });

        lf.setException(intentionalException);

        assertThat(cf).isDone();
        assertThat(cf).isNotCancelled();
        assertThat(cf).isNotCompletedExceptionally();

        assertThat(cf).isCompletedWithValue(value);
        assertThat(foundException.get()).isSameAs(intentionalException);
    }

    @Test
    public void forwardCancelShouldWork() {
        SettableFuture<String> lf = SettableFuture.create();
        CompletableFuture<String> cf = MoreFutures.toCompletableFuture(lf);

        lf.cancel(true);

        assertThat(cf).isDone();
        assertThat(cf).isCancelled();
        assertThat(cf).isCompletedExceptionally();
    }

    @Test
    public void backwardsCancelShouldWork() {
        SettableFuture<String> lf = SettableFuture.create();
        CompletableFuture<String> cf = MoreFutures.toCompletableFuture(lf);

        cf.cancel(true);

        assertThat(lf.isDone()).isTrue();
        assertThat(lf.isCancelled()).isTrue();
    }

    @Test
    public void backwardsCancellationExceptionShouldWork() {
        SettableFuture<String> lf = SettableFuture.create();
        CompletableFuture<String> cf = MoreFutures.toCompletableFuture(lf);

        cf.completeExceptionally(new CancellationException());

        assertThat(lf.isDone()).isTrue();
        assertThat(lf.isCancelled()).isTrue();
    }

    private final class IntentionalException extends Exception {

    }
}

