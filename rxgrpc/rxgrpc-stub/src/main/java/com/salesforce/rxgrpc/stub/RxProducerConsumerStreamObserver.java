/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import io.grpc.stub.ClientCallStreamObserver;
import io.reactivex.Flowable;

public class RxProducerConsumerStreamObserver<TRequest, TResponse> extends RxConsumerStreamObserver<TRequest, TResponse> {
    private Flowable<TRequest> rxProducer;

    public RxProducerConsumerStreamObserver(Flowable<TRequest> rxProducer) {
        this.rxProducer = rxProducer;
    }

    @Override
    public void beforeStart(ClientCallStreamObserver<TRequest> requestStream) {
        super.beforeStart(requestStream);
        rxProducer.subscribe(new FlowableBackpressureOnReadyHandler<>(requestStream));
    }
}
