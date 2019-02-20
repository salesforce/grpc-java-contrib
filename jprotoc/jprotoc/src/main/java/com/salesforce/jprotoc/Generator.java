/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.jprotoc;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.compiler.PluginProtos;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generator is the base class for all protoc generators managed by {@link ProtocPlugin}.
 */
public abstract class Generator {
    private static MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    /**
     * Processes a generator request into a set of files to output.
     *
     * @deprecated use {@link #generateFiles(PluginProtos.CodeGeneratorRequest)} and return a List instead of a Stream.
     * @param request The raw generator request from protoc.
     * @return The completed files to write out.
     */
    @Deprecated()
    public Stream<PluginProtos.CodeGeneratorResponse.File> generate(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
        return Stream.empty();
    }

    /**
     * Processes a generator request into a set of files to output.
     *
     * @param request The raw generator request from protoc.
     * @return The completed files to write out.
     */
    public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
        return Collections.emptyList();
    }

    /**
     * Executes a mustache template against a generatorContext object to generate an output string.
     * @param resourcePath Embedded resource template to use.
     * @param generatorContext Context object to bind the template to.
     * @return The string that results.
     */
    protected String applyTemplate(@Nonnull String resourcePath, @Nonnull Object generatorContext) {
        Preconditions.checkNotNull(resourcePath, "resourcePath");
        Preconditions.checkNotNull(generatorContext, "generatorContext");

        InputStream resource = MustacheFactory.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resource == null) {
            throw new RuntimeException("Could not find resource " + resourcePath);
        }

        InputStreamReader resourceReader = new InputStreamReader(resource, Charsets.UTF_8);
        Mustache template = mustacheFactory.compile(resourceReader, resourcePath);
        return template.execute(new StringWriter(), generatorContext).toString();
    }

    /**
     * Creates a protobuf file message from a given name and content.
     * @param fileName The name of the file to generate.
     * @param fileContent The content of the generated file.
     * @return The protobuf file.
     */
    protected PluginProtos.CodeGeneratorResponse.File makeFile(String fileName, String fileContent) {
        return PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(fileName)
                .setContent(fileContent)
                .build();
    }

    /**
     * Creates a protobuf file message from a given name and content.
     * @param fileName The name of the file to generate.
     * @param fileContent The content of the generated file.
     * @return The protobuf file.
     */
    protected PluginProtos.CodeGeneratorResponse.File makeFile(String fileName, byte[] fileContent) {
        return PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(fileName)
                .setContentBytes(ByteString.copyFrom(fileContent))
                .build();
    }
}
