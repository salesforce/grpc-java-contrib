/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.servicelibs.canteen.it;

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class CanteenExecutableIT {

    @Test
    public void stdoutWorks() throws Exception {
        File exe = getExecutable(getPlatformClassifier());

        Commandline commandLine = new Commandline();
        commandLine.setExecutable(exe.getCanonicalPath());
        commandLine.addArguments(new String[] {"1"});

        StringWriter stdOut = new StringWriter();
        StringWriter stdErr = new StringWriter();

        int returnCode = CommandLineUtils.executeCommandLine(commandLine, new WriterStreamConsumer(stdOut), new WriterStreamConsumer(stdErr));

        assertThat(returnCode).withFailMessage("Unexpected return code").isEqualTo(0);
        assertThat(stdOut.toString()).isEqualTo("success\n");
        assertThat(stdErr.toString()).isEmpty();
    }

    @Test
    public void stderrWorks() throws Exception {
        File exe = getExecutable(getPlatformClassifier());

        Commandline commandLine = new Commandline();
        commandLine.setExecutable(exe.getCanonicalPath());
        commandLine.addArguments(new String[] {"0"});

        StringWriter stdOut = new StringWriter();
        StringWriter stdErr = new StringWriter();

        int returnCode = CommandLineUtils.executeCommandLine(commandLine, new WriterStreamConsumer(stdOut), new WriterStreamConsumer(stdErr));

        assertThat(returnCode).withFailMessage("Unexpected return code").isEqualTo(42);
        assertThat(stdOut.toString()).isEmpty();
        assertThat(stdErr.toString()).isEqualTo("failure\n");
    }

    @Test
    public void stdinWorks() throws Exception {
        File exe = getExecutable(getPlatformClassifier());

        Commandline commandLine = new Commandline();
        commandLine.setExecutable(exe.getCanonicalPath());

        StringWriter stdOut = new StringWriter();
        StringWriter stdErr = new StringWriter();
        InputStream stdIn = new ByteArrayInputStream("1\n".getBytes(StandardCharsets.US_ASCII));

        int returnCode = CommandLineUtils.executeCommandLine(commandLine, stdIn,  new WriterStreamConsumer(stdOut), new WriterStreamConsumer(stdErr));

        assertThat(returnCode).withFailMessage("Unexpected return code").isEqualTo(0);
        assertThat(stdOut.toString()).endsWith("success\n");
        assertThat(stdErr.toString()).isEmpty();
    }

    private String getPlatformClassifier() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows-x86_64";
        } else if (os.contains("mac")) {
            return "osx-x86_64";
        } else if (os.contains("nix") || os.contains("nux")) {
            return "linux-x86_64";
        } else {
            fail("Unknown operating system " + os);
            return null;
        }
    }

    private File getExecutable(String classifier) throws IOException {
        File target = new File("./target");
        File[] match = target.listFiles((dir, name) -> name.endsWith(classifier + ".exe"));
        if (match != null) {
            return match[0].getCanonicalFile();
        } else {
            fail("Executable with classifier " + classifier + " not found");
            return null;
        }
    }
}
