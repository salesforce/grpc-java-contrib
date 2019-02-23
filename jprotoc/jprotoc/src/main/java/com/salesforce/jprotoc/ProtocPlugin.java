/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.jprotoc;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.compiler.PluginProtos;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.*;

/**
 * ProtocPlugin is the main entry point for running one or more java-base protoc plugins. This class handles
 * I/O marshaling and error reporting.
 */
public final class ProtocPlugin {
    private ProtocPlugin() {

    }

    /**
     * Apply a single generator to the parsed proto descriptor.
     * @param generator The generator to run.
     */
    public static void generate(@Nonnull Generator generator) {
        checkNotNull(generator, "generator");
        generate(Collections.singletonList(generator));
    }

    /**
     * Apply multiple generators to the parsed proto descriptor, aggregating their results.
     * @param generators The list of generators to run.
     */
    public static void generate(@Nonnull List<Generator> generators) {
        generate(generators, Collections.emptyList());
    }

    /**
     * Apply multiple generators to the parsed proto descriptor, aggregating their results.
     * Also register the given extensions so they may be processed by the generator.
     *
     * @param generators The list of generators to run.
     * @param extensions The list of extensions to register.
     */
    public static void generate(
            @Nonnull List<Generator> generators, List<GeneratedExtension> extensions) {
        checkNotNull(generators, "generators");
        checkArgument(!generators.isEmpty(), "generators.isEmpty()");
        checkNotNull(extensions, "extensions");

        // As per https://developers.google.com/protocol-buffers/docs/reference/java-generated#extension,
        // extensions must be registered in order to be processed.
        ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
        for (GeneratedExtension extension : extensions) {
            extensionRegistry.add(extension);
        }

        try {
            // Parse the input stream to extract the generator request
            byte[] generatorRequestBytes = ByteStreams.toByteArray(System.in);
            PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(
                    generatorRequestBytes, extensionRegistry);

            PluginProtos.CodeGeneratorResponse response = generate(generators, request);
            response.writeTo(System.out);

        } catch (GeneratorException ex) {
            try {
                PluginProtos.CodeGeneratorResponse
                        .newBuilder()
                        .setError(ex.getMessage())
                        .build()
                        .writeTo(System.out);
            } catch (IOException ex2) {
                abort(ex2);
            }
        } catch (Throwable ex) { // Catch all the things!
            abort(ex);
        }
    }

    /**
     * Debug a single generator using the parsed proto descriptor.
     * @param generator The generator to run.
     * @param dumpPath The path to a descriptor dump on the filesystem.
     */
    public static void debug(@Nonnull Generator generator, @Nonnull String dumpPath) {
        checkNotNull(generator, "generator");
        debug(Collections.singletonList(generator), dumpPath);
    }

    /**
     * Debug multiple generators using the parsed proto descriptor, aggregating their results.
     * @param generators The list of generators to run.
     * @param dumpPath The path to a descriptor dump on the filesystem.
     */
    public static void debug(@Nonnull List<Generator> generators, @Nonnull String dumpPath) {
        debug(generators, Collections.emptyList(), dumpPath);
    }

    /**
     * Debug multiple generators using the parsed proto descriptor, aggregating their results.
     * Also register the given extensions so they may be processed by the generator.
     *
     * @param generators The list of generators to run.
     * @param extensions The list of extensions to register.
     * @param dumpPath The path to a descriptor dump on the filesystem.
     */
    public static void debug(
            @Nonnull List<Generator> generators,
            List<GeneratedExtension> extensions,
            @Nonnull String dumpPath) {
        checkNotNull(generators, "generators");
        checkArgument(!generators.isEmpty(), "generators.isEmpty()");
        checkNotNull(extensions, "extensions");
        checkNotNull(dumpPath, "dumpPath");

        // As per https://developers.google.com/protocol-buffers/docs/reference/java-generated#extension,
        // extensions must be registered in order to be processed.
        ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
        for (GeneratedExtension extension : extensions) {
            extensionRegistry.add(extension);
        }

        try {
            byte[] generatorRequestBytes = ByteStreams.toByteArray(new FileInputStream(new File(dumpPath)));
            PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(
                    generatorRequestBytes, extensionRegistry);

            PluginProtos.CodeGeneratorResponse response = generate(generators, request);

            // Print error if present
            if (!Strings.isNullOrEmpty(response.getError())) {
                System.err.println(response.getError());
            }

            // Write files if present
            for (PluginProtos.CodeGeneratorResponse.File file : response.getFileList()) {
                File outFile;
                if (Strings.isNullOrEmpty(file.getInsertionPoint())) {
                    outFile = new File(file.getName());
                } else {
                    // Append insertion point to file name
                    String name = Files.getNameWithoutExtension(file.getName()) +
                            "-" +
                            file.getInsertionPoint() +
                            Files.getFileExtension(file.getName());
                    outFile = new File(name);
                }

                Files.createParentDirs(outFile);
                Files.write(file.getContent(), outFile, Charsets.UTF_8);
                Files.write(file.getContentBytes().toByteArray(), outFile);
            }

        } catch (Throwable ex) { // Catch all the things!
            ex.printStackTrace();
        }
    }

    private static PluginProtos.CodeGeneratorResponse generate(
            @Nonnull List<Generator> generators,
            @Nonnull PluginProtos.CodeGeneratorRequest request) {
        checkNotNull(generators, "generators");
        checkArgument(!generators.isEmpty(), "generators.isEmpty()");
        checkNotNull(request, "request");

        // Run each file generator, collecting the output
        Stream<PluginProtos.CodeGeneratorResponse.File> oldWay = generators
                .stream()
                .flatMap(gen -> gen.generate(request));

        Stream<PluginProtos.CodeGeneratorResponse.File> newWay = generators
                .stream()
                .flatMap(gen -> gen.generateFiles(request).stream());

        // Send the files back to protoc
        return PluginProtos.CodeGeneratorResponse
                .newBuilder()
                .addAllFile(Stream.concat(oldWay, newWay).collect(Collectors.toList()))
                .build();
    }

    private static void abort(Throwable ex) {
        ex.printStackTrace(System.err);
        System.exit(1);
    }
}
