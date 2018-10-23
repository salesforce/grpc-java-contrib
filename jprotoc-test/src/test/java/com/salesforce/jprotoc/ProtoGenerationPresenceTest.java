package com.salesforce.jprotoc;

import com.example.v2.FrontendGrpc8;
import my.someparameters.SomeParameterOuterClass;
import nested.NestedGrpc8;
import nested.NestedOuterClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test verifies the existence of generated proto stubs. If generation is successful, the classes will be
 * present to compile against.
 */
public class ProtoGenerationPresenceTest {
    @Test
    public void greeterGrpc8Exists() {
        assertThat(GreeterGrpc8.class).isNotNull();
    }

    @Test
    public void someParameterExists() {
        assertThat(SomeParameterOuterClass.SomeParameter.class).isNotNull();
    }

    @Test
    public void frontendGrpc8Exists() {
        assertThat(FrontendGrpc8.class).isNotNull();
    }

    @Test
    public void nestedGrpc8Exists() {
        assertThat(NestedGrpc8.class).isNotNull();
        assertThat(NestedOuterClass.Outer.MiddleAA.Inner.class).isNotNull();
        assertThat(NestedOuterClass.Outer.MiddleBB.Inner.class).isNotNull();
    }

    @Test
    public void currentTimeGrpc8Exists() {
        assertThat(CurrentTimeGrpc8.class).isNotNull();
    }
}
