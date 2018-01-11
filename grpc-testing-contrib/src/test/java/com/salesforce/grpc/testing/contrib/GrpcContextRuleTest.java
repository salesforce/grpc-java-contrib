/*
 *  Copyright (c) 2018, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.testing.contrib;

import io.grpc.Context;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

public class GrpcContextRuleTest {
    @Test
    public void ruleSetsContextToRoot() {
        Context.current().withValue(Context.key("foo"), "bar").run(() -> {
            assertThat(Context.current()).isNotEqualTo(Context.ROOT);

            try {
                GrpcContextRule rule = new GrpcContextRule();
                rule.apply(new Statement() {
                    @Override
                    public void evaluate() {
                        assertThat(Context.current()).isEqualTo(Context.ROOT);
                    }
                }, Description.createTestDescription(GrpcContextRuleTest.class, "ruleSetsContextToRoot"))
                .evaluate();
            } catch (Throwable throwable) {
                fail(throwable.getMessage());
            }
        });
    }

    @Test
    public void ruleFailsIfContextLeaks() {
        Context.current().withValue(Context.key("foo"), "bar").run(() -> {
            assertThat(Context.current()).isNotEqualTo(Context.ROOT);

            assertThatThrownBy(() -> {
                GrpcContextRule rule = new GrpcContextRule();
                rule.apply(new Statement() {
                    @Override
                    public void evaluate() {
                        // Leak context
                        Context.current().withValue(Context.key("cheese"), "baz").attach();
                    }
                }, Description.createTestDescription(GrpcContextRuleTest.class, "ruleSetsContextToRoot"))
                .evaluate();
            }).isInstanceOf(AssertionError.class).hasMessageContaining("Test is leaking context");
        });
    }
}
