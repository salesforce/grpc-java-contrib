package com.salesforce.jprotoc;

import com.google.common.io.ByteStreams;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests used a dumped descriptor set to load a {@link ProtoTypeMap} and verify its contents.
 *
 * You must run {@code mvn generate-test-sources} before running these tests in an IDE.
 */
public class ProtoTypeMapTest {
    private static ProtoTypeMap protoTypeMap;


    @BeforeClass
    public static void buildProtoTypeMap() throws IOException {
        // Dump file generated during the maven generate-test-sources phase
        final String dumpPath = "target/generated-test-sources/protobuf/dump/descriptor_dump";

        byte[] generatorRequestBytes = ByteStreams.toByteArray(new FileInputStream(new File(dumpPath)));
        PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(
                generatorRequestBytes, ExtensionRegistry.newInstance());
        List<DescriptorProtos.FileDescriptorProto> fileProtos = request.getProtoFileList();
        protoTypeMap =  ProtoTypeMap.of(fileProtos);
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
        assertProtoTypeMapping(".helloworld.HelloRequest", HelloWorldProto.HelloRequest.class);
        assertProtoTypeMapping(".helloworld.HelloResponse", HelloWorldProto.HelloResponse.class);
        assertProtoTypeMapping(".my.someparameters.SomeParameter", my.someparameters.SomeParameterOuterClass.SomeParameter.class);
    }

    /**
     * Verify that nested proto message types map correctly.
     */
    @Test
    public void nestedTypeMappings() {
        assertProtoTypeMapping(".nested.Outer", nested.NestedOuterClass.Outer.class);
        assertProtoTypeMapping(".nested.Outer.MiddleAA", nested.NestedOuterClass.Outer.MiddleAA.class);
        assertProtoTypeMapping(".nested.Outer.MiddleAA.Inner", nested.NestedOuterClass.Outer.MiddleAA.Inner.class);
        assertProtoTypeMapping(".nested.Outer.MiddleBB", nested.NestedOuterClass.Outer.MiddleBB.class);
        assertProtoTypeMapping(".nested.Outer.MiddleBB.Inner", nested.NestedOuterClass.Outer.MiddleBB.Inner.class);
    }

    /**
     * Verify that nested proto message types map correctly when {@code option java_multiple_files = true}.
     */
    @Test
    public void nestedTypeMappingsMultipleFiles() {
        assertProtoTypeMapping(".nested_multiple_files.Outer", nested_multiple_files.Outer.class);
        assertProtoTypeMapping(".nested_multiple_files.Outer.MiddleAA", nested_multiple_files.Outer.MiddleAA.class);
        assertProtoTypeMapping(".nested_multiple_files.Outer.MiddleAA.Inner", nested_multiple_files.Outer.MiddleAA.Inner.class);
        assertProtoTypeMapping(".nested_multiple_files.Outer.MiddleBB", nested_multiple_files.Outer.MiddleBB.class);
        assertProtoTypeMapping(".nested_multiple_files.Outer.MiddleBB.Inner", nested_multiple_files.Outer.MiddleBB.Inner.class);
    }

    /**
     * Verify that types with nested enums sharing the same name as a top-level type don't conflict.
     */
    @Test
    public void nestedEnumsDoNotConflictWithMessages() {
        assertProtoTypeMapping(".nested_overlap.Foo", nested_overlap.NestedEnumSameName.Foo.class);
        assertProtoTypeMapping(".nested_overlap.Outer.Foo", nested_overlap.NestedEnumSameName.Outer.Foo.class);
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

    @Test
    public void wonkyCasedNames() {
        assertProtoTypeMapping(".HELLOworld.HelloUPPERRequest", HELLOworld.HelloUPPERRequest.class);
    }

    private void assertProtoTypeMapping(String protoTypeName, Class clazz) {
        assertThat(protoTypeMap.toJavaTypeName(protoTypeName)).isEqualTo(fileNameToLexicalName(clazz.getName()));
    }

    private String fileNameToLexicalName(String fileName) {
        return fileName.replace("$", ".");
    }
}
