/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.reactivex.Flowable;

import java.util.function.Consumer;

/**
 * RxStreamObserver.
 * @param <TProducer>
 * @param <TConsumer>
 */
public class RxFlowableStreamObserver<TProducer, TConsumer> extends RxStreamObserver<TConsumer>  implements ClientResponseObserver<TProducer, TConsumer> {
    private Flowable<TProducer> rxProducer;

    public RxFlowableStreamObserver(Flowable<TProducer> rxProducer, Consumer<TConsumer> onNext, Consumer<Throwable> onError, Runnable onCompleted) {
        super(onNext, onError, onCompleted);
        this.rxProducer = rxProducer;
    }

    @Override
    public void beforeStart(ClientCallStreamObserver<TProducer> producerStream) {
        // Subscribe to the rxProducer with an adapter to a gRPC StreamObserver that respects backpressure
        // signals from the underlying gRPC client transport.
        rxProducer.subscribe(new FlowableBackpressureOnReadyHandler<>(producerStream));
    }
}