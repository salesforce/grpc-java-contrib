/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.jprotoc;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.compiler.PluginProtos;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

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
        Preconditions.checkNotNull(generator, "generator");
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
        Preconditions.checkNotNull(generators, "generators");
        Preconditions.checkArgument(!generators.isEmpty(), "generators.isEmpty()");
        Preconditions.checkNotNull(extensions, "extensions");

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

            // Run each file generator, collecting the output
            List<PluginProtos.CodeGeneratorResponse.File> outputFiles = generators
                    .stream()
                    .flatMap(gen -> gen.generate(request))
                    .collect(Collectors.toList());

            // Send the files back to protoc
            PluginProtos.CodeGeneratorResponse response = PluginProtos.CodeGeneratorResponse
                    .newBuilder()
                    .addAllFile(outputFiles)
                    .build();
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

    private static void abort(Throwable ex) {
        ex.printStackTrace(System.err);
        System.exit(1);
    }
}
