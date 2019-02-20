/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.spring;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.DeferredResultMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * GuavaLFReturnValueHandler teaches Spring Web how to deal with Controller methods that return {@link ListenableFuture}.
 * This allows you to use `ListenableFuture`-based logic end-to-end to build non-blocking asynchronous mvc services on
 * top of gRPC.
 *
 * To enable GuavaLFReturnValueHandler, wire it up as a spring {@code {@literal @}Bean}.
 *
 * <blockquote><pre><code>
 * {@literal @}Bean
 * public GuavaLFReturnValueHandler GuavaLFReturnValueHandler(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
 *     return new GuavaLFReturnValueHandler().install(requestMappingHandlerAdapter);
 * }
 * </code></pre></blockquote>
 *
 * Once installed, Spring {@code {@literal @}Controller} operations can return a {@link ListenableFuture}s
 * directly.
 *
 * <blockquote><pre><code>
 * {@literal @}Controller
 * public class MyController {
 *     {@literal @}RequestMapping(method = RequestMethod.GET, value = "/home")
 *     ListenableFuture<ModelAndView> home(HttpServletRequest request, Model model) {
 *         // work that returns a ListenableFuture...
 *     }
 * }
 * </code></pre></blockquote>
 *
 * Heavily inspired by https://github.com/AndreasKl/spring-boot-mvc-completablefuture
 */
public class GuavaLFReturnValueHandler implements HandlerMethodReturnValueHandler {
    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return ListenableFuture.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest) throws Exception {
        if (returnValue == null) {
            mavContainer.setRequestHandled(true);
            return;
        }

        final DeferredResult<Object> deferredResult = new DeferredResult<>();
        @SuppressWarnings("unchecked")
        ListenableFuture<Object> futureValue = (ListenableFuture<Object>) returnValue;
        Futures.addCallback(futureValue, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {
                deferredResult.setResult(result);
            }

            @Override
            public void onFailure(Throwable ex) {
                deferredResult.setErrorResult(ex);
            }
        });

        startDeferredResultProcessing(mavContainer, webRequest, deferredResult);
    }

    @VisibleForTesting
    protected void startDeferredResultProcessing(ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
                                       final DeferredResult<Object> deferredResult) throws Exception {
        WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(deferredResult, mavContainer);
    }

    // ===========================
    //        INSTALLATION
    // ===========================

    public GuavaLFReturnValueHandler install(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        final List<HandlerMethodReturnValueHandler> originalHandlers = new ArrayList<>(
                requestMappingHandlerAdapter.getReturnValueHandlers());

        final int deferredPos = indexOfType(originalHandlers, DeferredResultMethodReturnValueHandler.class);
        // Add our handler directly after the deferred handler.
        originalHandlers.add(deferredPos + 1, this);

        requestMappingHandlerAdapter.setReturnValueHandlers(originalHandlers);

        return this;
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
