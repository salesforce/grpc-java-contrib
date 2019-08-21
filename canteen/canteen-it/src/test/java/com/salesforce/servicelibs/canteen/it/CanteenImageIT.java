/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.servicelibs.canteen.it;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class CanteenImageIT {

    @Test
    public void linuxBootstrapApplied() {
        byte[] magic = new byte[] {0x7F, 0x45, 0x4C, 0x46}; // .ELF
        assertMagic("linux-x86_64", magic, 0);
    }

    @Test
    public void macosBootstrapApplied() {
        byte[] magic = new byte[] {(byte) 0xCF, (byte) 0xFA, (byte) 0xED, (byte) 0xFE}; // CF FA ED FE
        assertMagic("osx-x86_64", magic, 0);
    }

    @Test
    public void windowsBootstrapApplied() {
        byte[] magic = "This program cannot be run in DOS mode.".getBytes(StandardCharsets.US_ASCII);
        assertMagic("windows-x86_64", magic, 0x4E);
    }

    private void assertMagic(String classifier, byte[] magic, int offest) {
        try {
            File file = getExecutable(classifier);
            assertThat(file.exists()).withFailMessage("Not found: " + file.getPath()).isTrue();
            assertThat(containsMagic(file, magic, offest)).withFailMessage("Magic not found").isTrue();
        } catch (IOException ex) {
            fail("Not magic", ex);
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

    private boolean containsMagic(File file, byte[] magic, int offset) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[magic.length];
            fis.skip(offset);
            int l = fis.read(buf);

            if (l != buf.length) {
                fail("Could not read " + buf.length + " bytes from " + file.getCanonicalPath());
            }

            return Arrays.equals(buf, magic);
        }
    }
}
