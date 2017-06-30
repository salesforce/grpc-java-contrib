/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc.stub;


import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.subscribers.SafeSubscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * {@link io.reactivex.internal.operators.flowable.FlowableObserveOn} does not propagate events after {@code cancel()}
 * has been called. Normally this isn't a problem, but since RxGrpc needs to wait for the server to confirm
 * cancellation. By the time this happens, the observing thread has shut down.
 * <p>
 * {@code FlowableCancellationBridge} is needed to make up for the lost onComplete() response that gets lost when the
 * observing thread shuts down.
 * @param <T>
 */
public class FlowableCancellationBridge<T> extends Flowable<T> implements HasUpstreamPublisher<T> {
    private final Flowable<T> source;

    FlowableCancellationBridge(Flowable<T> source) {
        this.source = ObjectHelper.requireNonNull(source, "source is null");
    }

    @Override
    public final Publisher<T> source() {
        return source;
    }


    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new SafeSubscriber<>(new CancellationBridgeSubscriber(s)));
    }

    /**
     * CancellationBridgeSubscriber calls onComplete() when cancel() is called. Everything else is necessary
     * boilerplate.
     */
    private class CancellationBridgeSubscriber implements FlowableSubscriber<T>, Subscription {
        private final Subscriber<? super T> actual;
        private Subscription subscription;
        private boolean done;

        CancellationBridgeSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.subscription, s)) {
                subscription = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                actual.onNext(t);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (!done) {
                done = true;
                actual.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                actual.onComplete();
            }
        }

        @Override
        public void request(long n) {
            if (!SubscriptionHelper.validate(n)) {
                return;
            }
            subscription.request(n);
        }

        @Override
        public void cancel() {
            subscription.cancel();
            onComplete();
        }
    }
}
