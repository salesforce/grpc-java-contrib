/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.jprotoc;

import io.grpc.Channel;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.salesforce.jprotoc.HelloWorldProto.*;

import static org.junit.Assert.*;

/**
 * These tests validate the structure of the generated java emitted by the Jdk8Generator.
 */
public class Jdk8GeneratorStructureTest {
    @Test
    public void GeneratedClassesExists() throws Exception {
        Class<?> clazz = Class.forName("com.salesforce.jprotoc.GreeterGrpc8");
        assertNotNull(clazz);
        assertTrue(Modifier.isPublic(clazz.getModifiers()));
    }

    @Test
    public void GeneratedClassNotInstantiable() throws Exception {
        Class<?> clazz = Class.forName("com.salesforce.jprotoc.GreeterGrpc8");
        for (Constructor c : clazz.getConstructors()) {
            assertTrue(Modifier.isPrivate(c.getModifiers()));
        }
    }

    @Test
    public void GeneratedClassHasCompletableFutureStubMethod() throws Exception {
        Class<?> clazz = Class.forName("com.salesforce.jprotoc.GreeterGrpc8");
        Method stubMethod = clazz.getMethod("newCompletableFutureStub", Channel.class);
        assertNotNull(stubMethod);
        assertTrue(Modifier.isPublic(stubMethod.getModifiers()));
        assertTrue(Modifier.isStatic(stubMethod.getModifiers()));
    }

    @Test
    public void GeneratedClassHasStubSubClass() throws Exception {
        Class<?> clazz = Class.forName("com.salesforce.jprotoc.GreeterGrpc8$GreeterCompletableFutureStub");
        assertNotNull(clazz);
        assertTrue(Modifier.isPublic(clazz.getModifiers()));
        assertTrue(Modifier.isStatic(clazz.getModifiers()));
        assertTrue(Modifier.isFinal(clazz.getModifiers()));
    }

    @Test
    public void GeneratedStubClassDerivesFromAbstractStub() throws Exception {
        Class<?> clazz = Class.forName("com.salesforce.jprotoc.GreeterGrpc8$GreeterCompletableFutureStub");
        assertEquals("AbstractStub", clazz.getSuperclass().getSimpleName());
    }

    @Test
    public void GeneratedStubSubclassHasExpectedMethods() throws Exception {
        Class<?> clazz = Class.forName("com.salesforce.jprotoc.GreeterGrpc8$GreeterCompletableFutureStub");

        Method method = clazz.getMethod("sayHello", HelloRequest.class);
        assertNotNull(method);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertEquals("CompletableFuture", method.getReturnType().getSimpleName());
    }
}
