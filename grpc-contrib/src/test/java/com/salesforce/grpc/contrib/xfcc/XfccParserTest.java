/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.xfcc;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XfccParserTest {
    @Test
    public void parseSimpleHeaderWorks() {
        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "SAN=http://testclient.lyft.com";
        List<XForwardedClientCert> certs = XfccParser.parse(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getBy()).isEqualTo("http://frontend.lyft.com");
        assertThat(certs.get(0).getHash()).isEqualTo("468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688");
        assertThat(certs.get(0).getSan()).isEqualTo("http://testclient.lyft.com");
        assertThat(certs.get(0).getSubject()).isEmpty();
    }
    @Test
    public void parseCompoundHeaderWorks() {
        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;SAN=http://testclient.lyft.com," +
                "By=http://backend.lyft.com;Hash=9ba61d6425303443c0748a02dd8de688468ed33be74eee6556d90c0149c1309e;SAN=http://frontend.lyft.com";
        List<XForwardedClientCert> certs = XfccParser.parse(header);

        assertThat(certs.size()).isEqualTo(2);
        assertThat(certs.get(0).getBy()).isEqualTo("http://frontend.lyft.com");
        assertThat(certs.get(1).getBy()).isEqualTo("http://backend.lyft.com");
    }

    @Test
    public void quotedHeaderWorks() {
        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "Subject=\"/C=US/ST=CA/L=San Francisco/OU=Lyft/CN=Test Client\";SAN=http://testclient.lyft.com";
        List<XForwardedClientCert> certs = XfccParser.parse(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getSubject()).isEqualTo("/C=US/ST=CA/L=San Francisco/OU=Lyft/CN=Test Client");
    }

    @Test
    public void escapedQuotedHeaderWorks() {
        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "Subject=\"/C=US/ST=CA/L=\\\"San Francisco\\\"/OU=Lyft/CN=Test Client\";SAN=http://testclient.lyft.com";
        List<XForwardedClientCert> certs = XfccParser.parse(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getSubject()).isEqualTo("/C=US/ST=CA/L=\"San Francisco\"/OU=Lyft/CN=Test Client");
    }

    @Test
    public void mismatchedQuotesThrows() {
        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "Subject=\"/C=US/ST=CA/L=\\\"San Francisco\"/OU=Lyft/CN=Test Client\";SAN=http://testclient.lyft.com";

        assertThatThrownBy(() -> XfccParser.parse(header)).isInstanceOf(RuntimeException.class);
    }
}
