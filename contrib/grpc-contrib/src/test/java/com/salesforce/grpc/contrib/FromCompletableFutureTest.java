/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.*;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("ALL")
public class FromCompletableFutureTest {
    @Test
    public void incompleteShouldReportIncomplete() {
        CompletableFuture<String> cf = new CompletableFuture<>();
        ListenableFuture<String> lf = MoreFutures.fromCompletableFuture(cf);

        assertThat(lf.isDone()).isFalse();
        assertThat(lf.isCancelled()).isFalse();
    }

    @Test
    public void forwardCompletionShouldWork() throws Exception {
        final String value = UUID.randomUUID().toString();

        CompletableFuture<String> cf = new CompletableFuture<>();
        ListenableFuture<String> lf = MoreFutures.fromCompletableFuture(cf);

        cf.complete(value);

        assertThat(lf.isDone()).isTrue();
        assertThat(lf.isCancelled()).isFalse();;

        assertThat(MoreFutures.getDone(lf)).isEqualTo(value);
    }

    @Test
    public void forwardExceptionShouldWork() throws Exception {
        CompletableFuture<String> cf = new CompletableFuture<>();
        ListenableFuture<String> lf = MoreFutures.fromCompletableFuture(cf);

        Exception intentionalException = new IntentionalException();
        cf.completeExceptionally(intentionalException);

        assertThat(lf.isDone()).isTrue();
        assertThat(lf.isCancelled()).isFalse();

        assertThatThrownBy(() -> MoreFutures.getDone(lf)).hasCause(intentionalException);
    }

    @Test
    public void forwardExceptionShouldPropagate() throws Exception {
        final String value = UUID.randomUUID().toString();

        CompletableFuture<String> cf = new CompletableFuture<>();
        ListenableFuture<String> lf = MoreFutures.fromCompletableFuture(cf);

        Exception intentionalException = new IntentionalException();
        final AtomicReference<Throwable> foundException = new AtomicReference<>();

        lf = Futures.catching(lf, IntentionalException.class, ex -> {
            foundException.set(ex);
            return value;
        }, MoreExecutors.directExecutor());

        cf.completeExceptionally(intentionalException);

        assertThat(lf.isDone()).isTrue();
        assertThat(lf.isCancelled()).isFalse();

        assertThat(MoreFutures.getDone(lf)).isEqualTo(value);
        assertThat(foundException.get()).isSameAs(intentionalException);
    }

    @Test
    public void forwardCancelShouldWork() {
        CompletableFuture<String> cf = new CompletableFuture<>();
        ListenableFuture<String> lf = MoreFutures.fromCompletableFuture(cf);

        cf.cancel(true);

        assertThat(lf.isDone()).isTrue();
        assertThat(lf.isCancelled()).isTrue();
    }

    @Test
    public void backwardsCancelShouldWork() {
        CompletableFuture<String> cf = new CompletableFuture<>();
        ListenableFuture<String> lf = MoreFutures.fromCompletableFuture(cf);

        lf.cancel(true);

        assertThat(cf.isDone()).isTrue();
        assertThat(cf.isCancelled()).isTrue();
    }

    private final class IntentionalException extends Exception {

    }
}

