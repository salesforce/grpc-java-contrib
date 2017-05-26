/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import io.reactivex.Emitter;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * ObservableBridgeEmitter.
 * @param <T>
 */
public class ObservableBridgeEmitter<T> implements ObservableOnSubscribe<T>, Emitter<T> {
    private ObservableEmitter<T> emitter;

    @Override
    public void subscribe(ObservableEmitter<T> emitter) throws Exception {
        this.emitter = emitter;
    }

    @Override
    public void onNext(T t) {
        emitter.onNext(t);
    }

    @Override
    public void onError(Throwable throwable) {
        emitter.onError(throwable);
    }

    @Override
    public void onComplete() {
        emitter.onComplete();
    }
}
