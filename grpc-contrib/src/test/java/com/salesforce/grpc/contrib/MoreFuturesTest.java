/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.grpc.contrib;

import com.google.common.util.concurrent.*;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

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
}
