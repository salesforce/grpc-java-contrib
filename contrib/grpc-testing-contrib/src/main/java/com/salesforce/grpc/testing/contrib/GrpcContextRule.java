/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.testing.contrib;

import io.grpc.Context;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * {@code GrpcContextRule} is a JUnit {@link TestRule} that forcibly resets the gRPC
 * {@link Context} to {@link Context#ROOT} between every unit test.
 *
 * <p>This rule makes it easier to correctly implement correct unit tests by preventing the
 * accidental leakage of context state between tests.
 */
public class GrpcContextRule implements TestRule {
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Reset the gRPC context between test executions
                Context prev = Context.ROOT.attach();
                try {
                    base.evaluate();
                    if (Context.current() != Context.ROOT) {
                        Assert.fail("Test is leaking context state between tests! Ensure proper " +
                                "attach()/detach() pairing.");
                    }
                } finally {
                    Context.ROOT.detach(prev);
                }
            }
        };
    }
}