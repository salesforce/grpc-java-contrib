/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.jprotoc;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.protobuf.compiler.PluginProtos;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.stream.Stream;

/**
 * Generator is the base class for all protoc generators managed by {@link ProtocPlugin}.
 */
public abstract class Generator {
    private static MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    /**
     * Processes a generator request into a set of files to output.
     * @param request The raw generator request from protoc.
     * @return The completed files to write out.
     */
    public abstract Stream<PluginProtos.CodeGeneratorResponse.File> generate(PluginProtos.CodeGeneratorRequest request) throws GeneratorException;

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
}
