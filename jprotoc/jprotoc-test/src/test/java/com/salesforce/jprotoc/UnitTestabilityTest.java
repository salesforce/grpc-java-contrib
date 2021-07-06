package com.salesforce.jprotoc;

import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.jdk8.Jdk8Generator;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

public class UnitTestabilityTest {
    @Test
    public void verifyMavenDumpFileExists() throws IOException {
        File dumpfile = new File(ProtocPluginTesting.MAVEN_DUMP_PATH);
        System.out.println(dumpfile.getAbsolutePath());

        assertThat(dumpfile.exists()).isTrue();
        assertThat(dumpfile.isDirectory()).isFalse();
    }

    @Test
    public void verifyGeneratorWorks() throws IOException {
        PluginProtos.CodeGeneratorResponse response = ProtocPluginTesting.test(new Jdk8Generator(), ProtocPluginTesting.MAVEN_DUMP_PATH);
        assertThat(response.getError()).isNullOrEmpty();
        assertThat(response.getFileList()).isNotEmpty();
    }
}
