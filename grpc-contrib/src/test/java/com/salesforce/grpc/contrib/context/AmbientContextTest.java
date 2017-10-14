/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.Context;
import io.grpc.Metadata;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("Duplicates")
public class AmbientContextTest {
    @Before
    public void setUp() throws Exception {
        // Reset the gRPC context between test executions
        Context.ROOT.attach();
    }

    @Test
    public void initializeAttachesContext() {
        Context ctx = AmbientContext.initialize(Context.current());
        ctx.run(() -> assertThat(AmbientContext.current()).isNotNull());
    }

    @Test
    public void doubleInitializeThrows() {
        Context ctx = AmbientContext.initialize(Context.current());
        assertThatThrownBy(() -> AmbientContext.initialize(ctx)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void uninitializedContextThrows() {
        assertThatThrownBy(AmbientContext::current).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void contextFreezingWorks() {
        Metadata.Key<String> key = Metadata.Key.of("key", Metadata.ASCII_STRING_MARSHALLER);
        AmbientContext context = new AmbientContext();

        assertThat(context.isFrozen()).isFalse();
        Object freezeKey = context.freeze();
        assertThat(context.isFrozen()).isTrue();
        assertThatThrownBy(() -> context.put(key, "foo")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void contextDoubleFreezingThrows() {
        AmbientContext context = new AmbientContext();
        context.freeze();
        assertThatThrownBy(context::freeze).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void contextThawingWorks() {
        AmbientContext context = new AmbientContext();

        Object freezeKey = context.freeze();
        context.thaw(freezeKey);
        assertThat(context.isFrozen()).isFalse();
    }

    @Test
    public void contextThawingNotFrozenThrows() {
        AmbientContext context = new AmbientContext();
        assertThatThrownBy(() -> context.thaw(new Object())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void contextThawingWrongKeyThrows() {
        AmbientContext context = new AmbientContext();

        Object freezeKey = context.freeze();
        assertThatThrownBy(() -> context.thaw(new Object())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void contextScopeStackingWorks() {
        Metadata.Key<String> key = Metadata.Key.of("k", Metadata.ASCII_STRING_MARSHALLER);
        AmbientContext.initialize(Context.current()).run(() -> {
            AmbientContext.current().put(key, "outer");
            assertThat(AmbientContext.current().get(key)).isEqualTo("outer");

            AmbientContext.current().fork(Context.current()).run(() -> {
                AmbientContext.current().put(key, "inner");
                assertThat(AmbientContext.current().get(key)).isEqualTo("inner");
            });

            assertThat(AmbientContext.current().get(key)).isEqualTo("outer");
        });
    }
}
