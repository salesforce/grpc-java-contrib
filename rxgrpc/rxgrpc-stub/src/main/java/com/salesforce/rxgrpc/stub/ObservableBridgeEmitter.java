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
    private ObservableEmitter<T> observableEmitter;

    @Override
    public void subscribe(ObservableEmitter<T> observableEmitter) throws Exception {
        this.observableEmitter = observableEmitter;
    }

    @Override
    public void onNext(T t) {
        observableEmitter.onNext(t);
    }

    @Override
    public void onError(Throwable throwable) {
        observableEmitter.onError(throwable);
    }

    @Override
    public void onComplete() {
        observableEmitter.onComplete();
    }
}
