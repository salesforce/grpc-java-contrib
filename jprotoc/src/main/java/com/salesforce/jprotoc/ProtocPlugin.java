/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.jprotoc;

import com.google.common.io.ByteStreams;
import com.google.protobuf.compiler.PluginProtos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProtocPlugin is the main entry point for running one or more java-base protoc plugins. This class handles
 * I/O marshaling and error reporting.
 */
public class ProtocPlugin {
    public static void main(String[] args) {
        List<Generator> generators = new ArrayList<>();

        generators.add(new Jdk8Generator());
        // TODO: Future generators go here.

        new ProtocPlugin().generate(generators);
    }

    public void generate(List<Generator> generators) {
        try {
            // Parse the input stream to extract the generator request
            byte[] generatorRequestBytes = ByteStreams.toByteArray(System.in);
            PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(generatorRequestBytes);

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

    private void abort(Throwable ex) {
        ex.printStackTrace(System.err);
        System.exit(1);
    }
}
