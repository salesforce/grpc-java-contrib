/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import io.grpc.Context;
import io.grpc.testing.GrpcServerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class AmbientContextTest {
    @Rule public GrpcServerRule serverRule1 = new GrpcServerRule();
    @Rule public GrpcServerRule serverRule2 = new GrpcServerRule();

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
    public void contextAccessMethods() {
        fail("Not implemented");
    }

    @Test
    public void contextFreezingWorks() {
        fail("Not implemented");
    }

    @Test
    public void contextThawingWorks() {
        fail("Not implemented");
    }
}
