package com.salesforce.jprotoc;

import com.google.common.io.ByteStreams;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests used a dumped descriptor set to load a {@link ProtoTypeMap} and verify its contents.
 *
 * You must run {@code mvn generate-test-sources} before running these tests in an IDE.
 */
public class ProtoTypeMapMultipleFileTest {
    private static ProtoTypeMap protoTypeMap;


    @BeforeClass
    public static void buildProtoTypeMap() throws IOException {
        // Dump file generated during the maven generate-test-sources phase
        final String dumpPath = "target/generated-test-sources/protobuf/java/descriptor_dump";

        byte[] generatorRequestBytes = ByteStreams.toByteArray(new FileInputStream(new File(dumpPath)));
        PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(
                generatorRequestBytes, ExtensionRegistry.newInstance());
        protoTypeMap =  ProtoTypeMap.of(request.getProtoFileList());
    }


    @Test
    public void printCurrentPath() {
        System.out.println(new File(".").getAbsolutePath());
    }

    /**
     * Verify basic HelloWorld types
     */
    @Test
    public void helloWorldTypeMappings() {
        assertProtoTypeMapping(".multiple.helloworld.HelloRequest", com.salesforce.jprotoc.multiple.helloworld.HelloRequest.class);
        assertProtoTypeMapping(".multiple.helloworld.HelloResponse", com.salesforce.jprotoc.multiple.helloworld.HelloResponse.class);
        assertProtoTypeMapping(".multiple.my.someparameters.SomeParameter", multiple.my.someparameters.SomeParameter.class);
    }

    /**
     * Verify that nested proto message types map correctly.
     */
    @Test
    public void nestedTypeMappings() {
        assertProtoTypeMapping(".multiple.nested.Outer", multiple.nested.Outer.class);
        assertProtoTypeMapping(".multiple.nested.Outer.MiddleAA", multiple.nested.Outer.MiddleAA.class);
        assertProtoTypeMapping(".multiple.nested.Outer.MiddleAA.Inner", multiple.nested.Outer.MiddleAA.Inner.class);
        assertProtoTypeMapping(".multiple.nested.Outer.MiddleBB", multiple.nested.Outer.MiddleBB.class);
        assertProtoTypeMapping(".multiple.nested.Outer.MiddleBB.Inner", multiple.nested.Outer.MiddleBB.Inner.class);
    }

    /**
     * Verify that types with nested enums sharing the same name as a top-level type don't conflict.
     */
    @Test
    public void nestedEnumsDoNotConflictWithMessages() {
        assertProtoTypeMapping(".multiple.nested_overlap.Foo", multiple.nested_overlap.Foo.class);
        assertProtoTypeMapping(".multiple.nested_overlap.Outer.Foo", multiple.nested_overlap.Outer.Foo.class);
    }

    /**
     * Verify that proto types with invalid java class names are properly translated.
     */
    @Test
    public void invalidClassNames() {
        assertProtoTypeMapping(".com.salesforce.invalid.dollar.TimeResponse", com.salesforce.invalid.dollar.WeylandYutani.TimeResponse.class);
        assertProtoTypeMapping(".com.salesforce.invalid.plus.TimeResponse", com.salesforce.invalid.plus.WeylandYutani.TimeResponse.class);
        assertProtoTypeMapping(".com.salesforce.invalid.dot.TimeResponse", com.salesforce.invalid.dot.WeylandYutani.TimeResponse.class);
        assertProtoTypeMapping(".com.salesforce.invalid.number.TimeResponse", com.salesforce.invalid.number.Weyland9Yutani.TimeResponse.class);
        assertProtoTypeMapping(".com.salesforce.invalid.dash.TimeResponse", com.salesforce.invalid.dash.WeylandYutani.TimeResponse.class);
        assertProtoTypeMapping(".com.salesforce.invalid.underscore.TimeResponse", com.salesforce.invalid.underscore.WeylandYutani.TimeResponse.class);
        assertProtoTypeMapping(".com.salesforce.invalid.enye.TimeResponse", com.salesforce.invalid.enye.WeylandYutani.TimeResponse.class);
    }

    private void assertProtoTypeMapping(String protoTypeName, Class clazz) {
        assertThat(protoTypeMap.toJavaTypeName(protoTypeName)).isEqualTo(fileNameToLexicalName(clazz.getName()));
    }

    private String fileNameToLexicalName(String fileName) {
        return fileName.replace("$", ".");
    }
}
