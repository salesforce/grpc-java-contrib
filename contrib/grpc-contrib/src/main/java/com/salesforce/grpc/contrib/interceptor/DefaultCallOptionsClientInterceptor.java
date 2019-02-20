/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code DefaultCallOptionsClientInterceptor} applies specified gRPC {@code CallOptions} to every outbound request.
 * By default, {@code DefaultCallOptionsClientInterceptor} will not overwrite {@code CallOptions} already set on the
 * outbound request.
 *
 * <p>Example uses include:
 * <ul>
 *     <li>Applying a set of {@code CallCredentials} to every request from any stub.</li>
 *     <li>Applying a compression strategy to every request.</li>
 *     <li>Attaching a custom {@code CallOptions.Key<T>} to every request.</li>
 *     <li>Setting the {@code WaitForReady} bit on every request.</li>
 *     <li>Preventing upstream users from tweaking {@code CallOptions} values by forcibly overwriting the value with a
 *         specific default.</li>
 * </ul>
 */
public class DefaultCallOptionsClientInterceptor implements ClientInterceptor {
    private static final Field CUSTOM_OPTIONS_FIELD = getCustomOptionsField();

    private static Field getCustomOptionsField() {
        try {
            Field f;
            f = CallOptions.class.getDeclaredField("customOptions");
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private CallOptions defaultOptions;
    private boolean overwrite = false;

    /**
     * Constructs a {@code DefaultCallOptionsClientInterceptor}.
     * @param options the set of {@code CallOptions} to apply to every call
     */
    public DefaultCallOptionsClientInterceptor(CallOptions options) {
        this.defaultOptions = checkNotNull(options, "defaultOptions");
    }

    /**
     * Instructs the interceptor to overwrite {@code CallOptions} values even if they are already present on the
     * outbound request.
     *
     * @return this
     */
    public DefaultCallOptionsClientInterceptor overwriteExistingValues() {
        this.overwrite = true;
        return this;
    }

    public CallOptions getDefaultOptions() {
        return defaultOptions;
    }

    public void setDefaultOptions(CallOptions options) {
        this.defaultOptions = options;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return next.newCall(method, patchOptions(callOptions));
    }

    @VisibleForTesting
    CallOptions patchOptions(CallOptions baseOptions) {
        CallOptions patchedOptions = baseOptions;

        patchedOptions = patchOption(patchedOptions, CallOptions::getAuthority, CallOptions::withAuthority);
        patchedOptions = patchOption(patchedOptions, CallOptions::getCredentials, CallOptions::withCallCredentials);
        patchedOptions = patchOption(patchedOptions, CallOptions::getCompressor, CallOptions::withCompression);
        patchedOptions = patchOption(patchedOptions, CallOptions::getDeadline, CallOptions::withDeadline);
        patchedOptions = patchOption(patchedOptions, CallOptions::isWaitForReady, (callOptions, waitForReady) -> waitForReady ? callOptions.withWaitForReady() : callOptions.withoutWaitForReady());
        patchedOptions = patchOption(patchedOptions, CallOptions::getMaxInboundMessageSize, CallOptions::withMaxInboundMessageSize);
        patchedOptions = patchOption(patchedOptions, CallOptions::getMaxOutboundMessageSize, CallOptions::withMaxOutboundMessageSize);
        patchedOptions = patchOption(patchedOptions, CallOptions::getExecutor, CallOptions::withExecutor);

        for (ClientStreamTracer.Factory factory : defaultOptions.getStreamTracerFactories()) {
            patchedOptions = patchedOptions.withStreamTracerFactory(factory);
        }

        for (CallOptions.Key<Object> key : customOptionKeys(defaultOptions)) {
            patchedOptions = patchOption(patchedOptions, co -> co.getOption(key), (co, o) -> co.withOption(key, o));
        }

        return patchedOptions;
    }

    private <T> CallOptions patchOption(CallOptions baseOptions, Function<CallOptions, T> getter, BiFunction<CallOptions, T, CallOptions> setter) {
        T baseValue = getter.apply(baseOptions);
        if (baseValue == null || overwrite) {
            T patchValue = getter.apply(defaultOptions);
            if (patchValue != null) {
                return setter.apply(baseOptions, patchValue);
            }
        }

        return baseOptions;
    }

    @SuppressWarnings("unchecked")
    private List<CallOptions.Key<Object>> customOptionKeys(CallOptions callOptions) {
        try {
            Object[][] customOptions = (Object[][]) CUSTOM_OPTIONS_FIELD.get(callOptions);
            List<CallOptions.Key<Object>> keys = new ArrayList<>(customOptions.length);
            for (Object[] arr : customOptions) {
                keys.add((CallOptions.Key<Object>) arr[0]);
            }
            return keys;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
