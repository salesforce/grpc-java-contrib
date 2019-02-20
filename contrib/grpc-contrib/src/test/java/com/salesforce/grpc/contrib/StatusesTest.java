/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import io.grpc.Metadata;
import io.grpc.Status;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

public class StatusesTest {
    @Test
    public void hasStatusReturnsTrueStatusException() {
        Throwable t = Status.INTERNAL.asException();
        assertThat(Statuses.hasStatus(t)).isTrue();
    }

    @Test
    public void hasStatusReturnsTrueStatusRuntimeException() {
        Throwable t = Status.INTERNAL.asRuntimeException();
        assertThat(Statuses.hasStatus(t)).isTrue();
    }

    @Test
    public void hasStatusReturnsFalse() {
        Throwable t = new IllegalStateException();
        assertThat(Statuses.hasStatus(t)).isFalse();
    }

    @Test
    public void hasStatusCodeReturnsTrue() {
        Throwable t = Status.NOT_FOUND.asException();
        assertThat(Statuses.hasStatusCode(t, Status.Code.NOT_FOUND)).isTrue();
    }

    @Test
    public void hasStatusCodeReturnsFalseWrongCode() {
        Throwable t = Status.NOT_FOUND.asException();
        assertThat(Statuses.hasStatusCode(t, Status.Code.UNAUTHENTICATED)).isFalse();
    }

    @Test
    public void hasStatusCodeReturnsFalseWrongType() {
        Throwable t = new IllegalStateException();
        assertThat(Statuses.hasStatusCode(t, Status.Code.UNAUTHENTICATED)).isFalse();
    }

    @Test
    public void doWithStatusCallsAction() {
        Metadata trailers = new Metadata();
        Throwable t = Status.NOT_FOUND.asRuntimeException(trailers);
        AtomicBoolean called = new AtomicBoolean(false);

        Statuses.doWithStatus(t, (status, metadata) -> {
            assertThat(status.getCode()).isEqualTo(Status.Code.NOT_FOUND);
            assertThat(metadata).isEqualTo(trailers);
            called.set(true);
        });

        assertThat(called.get()).isTrue();
    }

    @Test
    public void doWithStatusThrowsWrongTypeAction() {
        Throwable t = new IllegalStateException();
        assertThatThrownBy(() -> Statuses.doWithStatus(t, (status, metadata) -> { }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void doWithStatusCallsFunction() {
        Metadata trailers = new Metadata();
        Throwable t = Status.NOT_FOUND.asRuntimeException(trailers);

        boolean called = Statuses.doWithStatus(t, (status, metadata) -> {
            assertThat(status.getCode()).isEqualTo(Status.Code.NOT_FOUND);
            assertThat(metadata).isEqualTo(trailers);
            return true;
        });

        assertThat(called).isTrue();
    }

    @Test
    public void doWithStatusThrowsWrongTypeFunction() {
        Throwable t = new IllegalStateException();
        assertThatThrownBy(() -> Statuses.doWithStatus(t, (status, metadata) -> true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
