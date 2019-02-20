/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.jprotoc.dump;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.compiler.PluginProtos;
import com.google.protobuf.util.JsonFormat;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtocPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Dumps the content of the input descriptor set to descriptor_dump.json.
 */
public class DumpGenerator extends Generator {
    public static void main(String[] args) {
        ProtocPlugin.generate(new DumpGenerator());
    }

    @Override
    public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
        try {
            return Arrays.asList(
                    makeFile("descriptor_dump", request.toByteArray()),
                    makeFile("descriptor_dump.json", JsonFormat.printer().print(request))
            );
        } catch (InvalidProtocolBufferException e) {
            throw new GeneratorException(e.getMessage());
        }
    }
}
