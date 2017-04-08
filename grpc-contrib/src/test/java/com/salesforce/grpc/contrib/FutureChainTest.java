/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.grpc.contrib;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.*;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FutureChainTest {
    @Test
    public void FutureChainShouldWork() throws Exception {
        UUID uuid = UUID.randomUUID();
        SettableFuture<UUID> uuidFuture = SettableFuture.create();

        ListenableFuture<Integer> future = FutureChain
                .startWith(uuidFuture, MoreExecutors.directExecutor())
                .transform(UUID::toString)
                .transformAsync(str -> Futures.immediateFuture(str.length()))
                .catching(Throwable.class, ex -> -1)
                .compile();

        assertFalse(future.isDone());
        assertFalse(future.isCancelled());

        uuidFuture.set(uuid);
        assertTrue(future.isDone());
        assertEquals((Integer)uuid.toString().length(), MoreFutures.getDone(future));
    }

    @Test
    public void FutureChainCatchingShouldWork() throws Exception {
        SettableFuture<UUID> uuidFuture = SettableFuture.create();

        ListenableFuture<Integer> future = FutureChain
                .startWith(uuidFuture, MoreExecutors.directExecutor())
                .transform(UUID::toString)
                .transformAsync(str -> Futures.immediateFuture(str.length()))
                .catching(Throwable.class, ex -> -1)
                .compile();

        assertFalse(future.isDone());
        assertFalse(future.isCancelled());

        uuidFuture.setException(new Exception());
        assertTrue(future.isDone());
        assertEquals(new Integer(-1), MoreFutures.getDone(future));
    }

    @Test
    public void FutureChainCallbacksShouldWork() throws Exception {
        UUID uuid = UUID.randomUUID();
        SettableFuture<UUID> uuidFuture = SettableFuture.create();
        AtomicInteger length = new AtomicInteger();
        AtomicReference<String> str = new AtomicReference<>();

        ListenableFuture<Integer> future = FutureChain
                .startWith(uuidFuture, MoreExecutors.directExecutor())
                .transform(UUID::toString)
                .addCallback(s -> str.set(s), e -> {})
                .transformAsync(s -> Futures.immediateFuture(s.length()))
                .catching(Throwable.class, ex -> -1)
                .addCallback(i -> length.set(i), e -> {})
                .compile();

        uuidFuture.set(uuid);
        assertEquals(uuid.toString(), str.get());
        assertEquals(uuid.toString().length(), length.get());
    }
}

