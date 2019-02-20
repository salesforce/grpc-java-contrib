/*
 *  Copyright (c) 2017, salesforce.com, inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AggregateServerInterceptorTest {
    @Test
    public void ConstructorFailsOnNull() {
        assertThatThrownBy(() -> new AggregateServerInterceptor((List<ServerInterceptor>)null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void ConstructorFailsOnEmpty() {
        assertThatThrownBy(AggregateServerInterceptor::new)
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AggregateServerInterceptor(new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void OneInterceptorIsCalled() {
        AtomicBoolean interceptorCalled = new AtomicBoolean(false);

        ServerInterceptor interceptor = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                interceptorCalled.set(true);
                return next.startCall(call, headers);
            }
        };

        ServerInterceptor aggregate = new AggregateServerInterceptor(interceptor);
        StubHandler<Object, Object> handler = new StubHandler<>();
        aggregate.interceptCall(null, new Metadata(), handler);

        assertThat(interceptorCalled.get()).isTrue();
        assertThat(handler.called).isTrue();
    }

    @Test
    public void TwoInterceptorsAreCalledInOrder() {
        AtomicBoolean interceptorOneCalled = new AtomicBoolean(false);
        AtomicBoolean interceptorTwoCalled = new AtomicBoolean(false);

        ServerInterceptor interceptorOne = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                interceptorOneCalled.set(true);
                return next.startCall(call, headers);
            }
        };

        ServerInterceptor interceptorTwo = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                assertThat(interceptorOneCalled.get()).isTrue();
                interceptorTwoCalled.set(true);
                return next.startCall(call, headers);
            }
        };

        ServerInterceptor aggregate = new AggregateServerInterceptor(interceptorOne, interceptorTwo);
        StubHandler<Object, Object> handler = new StubHandler<>();
        aggregate.interceptCall(null, new Metadata(), handler);

        assertThat(interceptorOneCalled.get()).isTrue();
        assertThat(interceptorTwoCalled.get()).isTrue();
        assertThat(handler.called).isTrue();
    }

    private class StubHandler<TReq, TResp> implements ServerCallHandler<TReq, TResp> {
        public boolean called;

        @Override
        public ServerCall.Listener<TReq> startCall(ServerCall<TReq, TResp> call, Metadata headers) {
            called = true;
            return null;
        }
    }
}
