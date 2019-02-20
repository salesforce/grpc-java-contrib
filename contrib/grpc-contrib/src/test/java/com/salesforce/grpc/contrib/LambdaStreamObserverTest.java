/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class LambdaStreamObserverTest {
    @Test
    public void OnNextDelegatesCorrectly() {
        AtomicReference<Object> called = new AtomicReference<>();
        Object val = new Object();

        LambdaStreamObserver<Object> obs = new LambdaStreamObserver<>(called::set, null, null);
        obs.onNext(val);

        assertThat(called.get()).isEqualTo(val);
    }

    @Test
    public void OnErrorDelegatesCorrectly() {
        AtomicReference<Throwable> called = new AtomicReference<>();
        Throwable val = new Exception();

        LambdaStreamObserver<Object> obs = new LambdaStreamObserver<>(null, called::set, null);
        obs.onError(val);

        assertThat(called.get()).isEqualTo(val);
    }

    @Test
    public void OnCompletedDelegatesCorrectly() {
        AtomicBoolean called = new AtomicBoolean(false);

        LambdaStreamObserver<Object> obs = new LambdaStreamObserver<>(null, null, () -> called.set(true));
        obs.onCompleted();

        assertThat(called.get()).isTrue();
    }
}
