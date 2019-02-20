/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.spring;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.DeferredResultMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GuavaLFReturnValueHandlerTest {
    @Autowired
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @Autowired
    GuavaLFReturnValueHandler handler;

    @Test
    public void handlerIsRegistered() {
        List<HandlerMethodReturnValueHandler> handlers = requestMappingHandlerAdapter.getReturnValueHandlers();
        assertThat(indexOfType(handlers, GuavaLFReturnValueHandler.class)).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void handlerIsAfterDeferredResultMethodReturnValueHandler() {
        List<HandlerMethodReturnValueHandler> handlers = requestMappingHandlerAdapter.getReturnValueHandlers();
        int lfHandlerIndex = indexOfType(handlers, GuavaLFReturnValueHandler.class);
        int drHandlerIndex = indexOfType(handlers, DeferredResultMethodReturnValueHandler.class);

        assertThat(lfHandlerIndex).isGreaterThan(drHandlerIndex);
    }

    @Test
    public void supportsType() {
        MethodParameter mp = mock(MethodParameter.class);
        when(mp.getParameterType()).thenAnswer((Answer<Class<ListenableFuture>>) x -> ListenableFuture.class);

        assertThat(handler.supportsReturnType(mp)).isTrue();
    }

    @Test
    public void handlesSuccess() throws Exception {
        final AtomicReference<Object> value = new AtomicReference<>();

        ListenableFuture<String> future = Futures.immediateFuture("42");

        GuavaLFReturnValueHandler handler = new GuavaLFReturnValueHandler() {
            @Override
            protected void startDeferredResultProcessing(ModelAndViewContainer mavContainer, NativeWebRequest webRequest, DeferredResult<Object> deferredResult) throws Exception {
                value.set(deferredResult.getResult());
            }
        };

        handler.handleReturnValue(future, null, null, null);
        assertThat(value.get()).isEqualTo("42");
    }

    @Test
    public void handlesFailure() throws Exception {
        final AtomicReference<Object> value = new AtomicReference<>();
        Exception ex = new Exception("This is bad");
        ListenableFuture<String> future = Futures.immediateFailedFuture(ex);

        GuavaLFReturnValueHandler handler = new GuavaLFReturnValueHandler() {
            @Override
            protected void startDeferredResultProcessing(ModelAndViewContainer mavContainer, NativeWebRequest webRequest, DeferredResult<Object> deferredResult) throws Exception {
                value.set(deferredResult.getResult());
            }
        };

        handler.handleReturnValue(future, null, null, null);
        assertThat(value.get()).isEqualTo(ex);
    }

    @Configuration
    static class TestConfiguration {
        @Bean
        public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
            return new RequestMappingHandlerAdapter();
        }

        @Bean
        public GuavaLFReturnValueHandler GuavaLFReturnValueHandler(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
            return new GuavaLFReturnValueHandler().install(requestMappingHandlerAdapter);
        }
    }

    private int indexOfType(final List<HandlerMethodReturnValueHandler> originalHandlers, Class<?> handlerClass) {
        for (int i = 0; i < originalHandlers.size(); i++) {
            final HandlerMethodReturnValueHandler valueHandler = originalHandlers.get(i);
            if (handlerClass.isAssignableFrom(valueHandler.getClass())) {
                return i;
            }
        }
        return -1;
    }
}
