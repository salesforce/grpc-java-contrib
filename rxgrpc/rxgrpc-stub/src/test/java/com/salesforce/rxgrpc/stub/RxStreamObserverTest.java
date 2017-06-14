/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class RxStreamObserverTest {
    @Test
    public void OnNextDelegatesCorrectly() {
        AtomicReference<Object> called = new AtomicReference<>();
        Object val = new Object();

        RxStreamObserver<Object> obs = new RxStreamObserver<>(called::set, null, null);
        obs.onNext(val);

        assertThat(called.get()).isEqualTo(val);
    }

    @Test
    public void OnErrorDelegatesCorrectly() {
        AtomicReference<Throwable> called = new AtomicReference<>();
        Throwable val = new Exception();

        RxStreamObserver<Object> obs = new RxStreamObserver<>(null, called::set, null);
        obs.onError(val);

        assertThat(called.get()).isEqualTo(val);
    }

    @Test
    public void OnCompletedDelegatesCorrectly() {
        AtomicBoolean called = new AtomicBoolean(false);

        RxStreamObserver<Object> obs = new RxStreamObserver<>(null, null, () -> called.set(true));
        obs.onCompleted();

        assertThat(called.get()).isTrue();
    }
}
