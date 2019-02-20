/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import io.grpc.CallOptions;
import io.grpc.ClientStreamTracer;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class DefaultCallOptionsClientInterceptorTest {
    @Test
    public void simpleValueTransfers() {
        CallOptions baseOptions = CallOptions.DEFAULT;
        CallOptions defaultOptions = CallOptions.DEFAULT.withAuthority("FOO");

        DefaultCallOptionsClientInterceptor interceptor = new DefaultCallOptionsClientInterceptor(defaultOptions);

        CallOptions patchedOptions = interceptor.patchOptions(baseOptions);

        assertThat(patchedOptions.getAuthority()).isEqualTo("FOO");
    }

    @Test
    public void clientStreamTracerTransfers() {
        ClientStreamTracer.Factory factory1 = new ClientStreamTracer.Factory() {};
        ClientStreamTracer.Factory factory2 = new ClientStreamTracer.Factory() {};

        CallOptions baseOptions = CallOptions.DEFAULT.withStreamTracerFactory(factory1);
        CallOptions defaultOptions = CallOptions.DEFAULT.withStreamTracerFactory(factory2);

        DefaultCallOptionsClientInterceptor interceptor = new DefaultCallOptionsClientInterceptor(defaultOptions);

        CallOptions patchedOptions = interceptor.patchOptions(baseOptions);

        assertThat(patchedOptions.getStreamTracerFactories()).containsExactly(factory1, factory2);
    }

    @Test
    public void customKeyTransfers() {
        CallOptions.Key<String> k1 = CallOptions.Key.of("k1", null);
        CallOptions.Key<String> k2 = CallOptions.Key.of("k2", null);

        CallOptions baseOptions = CallOptions.DEFAULT.withOption(k1, "FOO");
        CallOptions defaultOptions = CallOptions.DEFAULT.withOption(k2, "BAR");

        DefaultCallOptionsClientInterceptor interceptor = new DefaultCallOptionsClientInterceptor(defaultOptions);

        CallOptions patchedOptions = interceptor.patchOptions(baseOptions);

        assertThat(patchedOptions.getOption(k1)).isEqualTo("FOO");
        assertThat(patchedOptions.getOption(k2)).isEqualTo("BAR");
    }

    @Test
    public void noOverwriteWorks() {
        CallOptions baseOptions = CallOptions.DEFAULT.withAuthority("FOO");
        CallOptions defaultOptions = CallOptions.DEFAULT.withAuthority("BAR");

        DefaultCallOptionsClientInterceptor interceptor = new DefaultCallOptionsClientInterceptor(defaultOptions);

        CallOptions patchedOptions = interceptor.patchOptions(baseOptions);

        assertThat(patchedOptions.getAuthority()).isEqualTo("FOO");
    }

    @Test
    public void noOverwriteWorksCustomKeys() {
        CallOptions.Key<String> k1 = CallOptions.Key.of("k1", null);
        CallOptions.Key<String> k2 = CallOptions.Key.of("k2", null);
        CallOptions.Key<String> k3 = CallOptions.Key.of("k3", null);

        CallOptions baseOptions = CallOptions.DEFAULT.withOption(k1, "FOO").withOption(k3, "BAZ");
        CallOptions defaultOptions = CallOptions.DEFAULT.withOption(k2, "BAR").withOption(k3, "BOP");

        DefaultCallOptionsClientInterceptor interceptor = new DefaultCallOptionsClientInterceptor(defaultOptions);

        CallOptions patchedOptions = interceptor.patchOptions(baseOptions);

        assertThat(patchedOptions.getOption(k1)).isEqualTo("FOO");
        assertThat(patchedOptions.getOption(k2)).isEqualTo("BAR");
        assertThat(patchedOptions.getOption(k3)).isEqualTo("BAZ");
    }

    @Test
    public void overwriteWorks() {
        CallOptions baseOptions = CallOptions.DEFAULT.withAuthority("FOO");
        CallOptions defaultOptions = CallOptions.DEFAULT.withAuthority("BAR");

        DefaultCallOptionsClientInterceptor interceptor = new DefaultCallOptionsClientInterceptor(defaultOptions)
                .overwriteExistingValues();

        CallOptions patchedOptions = interceptor.patchOptions(baseOptions);

        assertThat(patchedOptions.getAuthority()).isEqualTo("BAR");
    }

    @Test
    public void overwriteWorksCustomKeys() {
        CallOptions.Key<String> k1 = CallOptions.Key.of("k1", null);
        CallOptions.Key<String> k2 = CallOptions.Key.of("k2", null);
        CallOptions.Key<String> k3 = CallOptions.Key.of("k3", null);

        CallOptions baseOptions = CallOptions.DEFAULT.withOption(k1, "FOO").withOption(k3, "BAZ");
        CallOptions defaultOptions = CallOptions.DEFAULT.withOption(k2, "BAR").withOption(k3, "BOP");

        DefaultCallOptionsClientInterceptor interceptor = new DefaultCallOptionsClientInterceptor(defaultOptions)
                .overwriteExistingValues();

        CallOptions patchedOptions = interceptor.patchOptions(baseOptions);

        assertThat(patchedOptions.getOption(k1)).isEqualTo("FOO");
        assertThat(patchedOptions.getOption(k2)).isEqualTo("BAR");
        assertThat(patchedOptions.getOption(k3)).isEqualTo("BOP");
    }
}
