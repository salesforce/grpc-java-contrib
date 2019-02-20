/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import io.grpc.*;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
public class DefaultDeadlineInterceptorTest {
    @Test
    public void interceptorShouldAddDeadlineWhenAbsent() {
        AtomicBoolean called = new AtomicBoolean(false);

        DefaultDeadlineInterceptor interceptor = new DefaultDeadlineInterceptor(Duration.ofHours(1));

        interceptor.interceptCall(null, CallOptions.DEFAULT, new Channel() {
            @Override
            public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
                called.set(true);
                assertThat(callOptions.getDeadline().timeRemaining(TimeUnit.MINUTES)).isEqualTo(59);
                return null;
            }

            @Override
            public String authority() {
                return null;
            }
        });

        assertThat(called.get()).isTrue();
    }

    @Test
    public void interceptorShouldNotModifyExplicitDeadline() {
        AtomicBoolean called = new AtomicBoolean(false);

        DefaultDeadlineInterceptor interceptor = new DefaultDeadlineInterceptor(Duration.ofHours(1));

        interceptor.interceptCall(null, CallOptions.DEFAULT.withDeadlineAfter(10, TimeUnit.HOURS), new Channel() {
            @Override
            public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
                called.set(true);
                assertThat(callOptions.getDeadline().timeRemaining(TimeUnit.HOURS)).isEqualTo(9);
                return null;
            }

            @Override
            public String authority() {
                return null;
            }
        });

        assertThat(called.get()).isTrue();
    }

    @Test
    public void interceptorShouldNotModifyContextDeadline() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);

        DefaultDeadlineInterceptor interceptor = new DefaultDeadlineInterceptor(Duration.ofHours(1));

        Context.current().withDeadlineAfter(10, TimeUnit.HOURS, Executors.newSingleThreadScheduledExecutor()).run(() -> {
            interceptor.interceptCall(null, CallOptions.DEFAULT, new Channel() {
                @Override
                public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
                    called.set(true);
                    assertThat(callOptions.getDeadline()).isNull();
                    return null;
                }

                @Override
                public String authority() {
                    return null;
                }
            });
        });

        assertThat(called.get()).isTrue();
    }
}
