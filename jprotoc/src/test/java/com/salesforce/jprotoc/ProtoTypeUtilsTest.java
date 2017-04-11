package com.salesforce.jprotoc;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

public class ProtoTypeUtilsTest {

    @Test
    public void classWithEnclosingClassAndJavaPackage() {
        final String className = randomAlphabetic(ThreadLocalRandom.current().nextInt(5, 10));
        final String enclosingClassName = randomAlphabetic(ThreadLocalRandom.current().nextInt(5, 10));
        final String javaPackage = randomAlphabetic(ThreadLocalRandom.current().nextInt(5, 10));

        assertThat(ProtoTypeMap.toJavaTypeName(className, enclosingClassName, javaPackage))
                .isEqualTo(javaPackage + "." + enclosingClassName + "." + className);
    }

    @Test
    public void classWithEnclosingClass() {
        final String className = randomAlphabetic(ThreadLocalRandom.current().nextInt(5, 10));
        final String enclosingClassName = randomAlphabetic(ThreadLocalRandom.current().nextInt(5, 10));

        assertThat(ProtoTypeMap.toJavaTypeName(className, enclosingClassName, null))
                .isEqualTo(enclosingClassName + "." + className);
    }

    @Test
    public void classWithJavaPackage() {
        final String className = randomAlphabetic(ThreadLocalRandom.current().nextInt(5, 10));
        final String javaPackage = randomAlphabetic(ThreadLocalRandom.current().nextInt(5, 10));

        assertThat(ProtoTypeMap.toJavaTypeName(className, null, javaPackage))
                .isEqualTo(javaPackage + "." + className);
    }
}
