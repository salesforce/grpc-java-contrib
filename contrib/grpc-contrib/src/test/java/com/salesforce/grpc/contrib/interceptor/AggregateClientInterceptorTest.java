/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import io.grpc.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

public class AggregateClientInterceptorTest {
    @Test
    public void ConstructorFailsOnNull() {
        assertThatThrownBy(() -> new AggregateClientInterceptor((List<ClientInterceptor>)null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void ConstructorFailsOnEmpty() {
        assertThatThrownBy(AggregateClientInterceptor::new)
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AggregateClientInterceptor(new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void OneInterceptorIsCalled() {
        AtomicBoolean interceptorCalled = new AtomicBoolean(false);

        ClientInterceptor interceptor = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                interceptorCalled.set(true);
                return next.newCall(method, callOptions);
            }
        };

        ClientInterceptor aggregate = new AggregateClientInterceptor(interceptor);
        StubChannel channel = new StubChannel();
        aggregate.interceptCall(null, CallOptions.DEFAULT, channel);

        assertThat(interceptorCalled.get()).isTrue();
        assertThat(channel.called).isTrue();
    }

    @Test
    public void TwoInterceptorsAreCalledInOrder() {
        AtomicBoolean interceptorOneCalled = new AtomicBoolean(false);
        AtomicBoolean interceptorTwoCalled = new AtomicBoolean(false);

        ClientInterceptor interceptorOne = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                interceptorOneCalled.set(true);
                return next.newCall(method, callOptions);
            }
        };

        ClientInterceptor interceptorTwo = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                // check for order
                assertThat(interceptorOneCalled.get()).isTrue();
                interceptorTwoCalled.set(true);
                return next.newCall(method, callOptions);
            }
        };

        ClientInterceptor aggregate = new AggregateClientInterceptor(interceptorOne, interceptorTwo);
        StubChannel channel = new StubChannel();
        aggregate.interceptCall(null, CallOptions.DEFAULT, channel);

        assertThat(interceptorOneCalled.get()).isTrue();
        assertThat(interceptorTwoCalled.get()).isTrue();
        assertThat(channel.called).isTrue();
    }

    private class StubChannel extends Channel {
        public boolean called;

        @Override
        public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
            called = true;
            return null;
        }

        @Override
        public String authority() {
            return null;
        }
    }
}
