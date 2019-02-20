/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
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
    public void parseLegacySanHeaderWorks() {
        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "SAN=http://testclient.lyft.com";
        List<XForwardedClientCert> certs = XfccParser.parse(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getBy()).isEqualTo("http://frontend.lyft.com");
        assertThat(certs.get(0).getHash()).isEqualTo("468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688");
        assertThat(certs.get(0).getSanUri()).isEqualTo("http://testclient.lyft.com");
        assertThat(certs.get(0).getSubject()).isEmpty();
        assertThat(certs.get(0).getSanDns()).isEmpty();
    }

    @Test
    public void parseSimpleHeaderWorks() {
        String header = "Hash=ebb216c5155a5fd8c8f082a07362b3c7b1a8ee2f98c20f6142b49fe5c2db90bd;DNS=test-tls-in;DNS=second-san;" +
                "DNS=third-san;Subject=\"OU=0:test-tls-in,CN=localhost\"";
        List<XForwardedClientCert> certs = XfccParser.parse(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getBy()).isEmpty();
        assertThat(certs.get(0).getHash()).isEqualTo("ebb216c5155a5fd8c8f082a07362b3c7b1a8ee2f98c20f6142b49fe5c2db90bd");
        assertThat(certs.get(0).getSanUri()).isEmpty();
        assertThat(certs.get(0).getSubject()).isEqualTo("OU=0:test-tls-in,CN=localhost");
        assertThat(certs.get(0).getSanDns()).containsExactly("test-tls-in", "second-san", "third-san");
    }

    @Test
    public void parseUriSanHeaderWorks() {
        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;Subject=\"/C=US/ST=CA/L=San Francisco/OU=Lyft/CN=Test Client\";URI=http://testclient.lyft.com";
        List<XForwardedClientCert> certs = XfccParser.parse(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getBy()).isEqualTo("http://frontend.lyft.com");
        assertThat(certs.get(0).getHash()).isEqualTo("468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688");
        assertThat(certs.get(0).getSanUri()).isEqualTo("http://testclient.lyft.com");
        assertThat(certs.get(0).getSubject()).isEqualTo("/C=US/ST=CA/L=San Francisco/OU=Lyft/CN=Test Client");
        assertThat(certs.get(0).getSanDns()).isEmpty();
    }

    @Test
    public void parseUriAndDnsSanHeaderWorks() {
        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;Subject=\"/C=US/ST=CA/L=San Francisco/OU=Lyft/CN=Test Client\";URI=http://testclient.lyft.com;DNS=lyft.com;DNS=www.lyft.com";
        List<XForwardedClientCert> certs = XfccParser.parse(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getBy()).isEqualTo("http://frontend.lyft.com");
        assertThat(certs.get(0).getHash()).isEqualTo("468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688");
        assertThat(certs.get(0).getSanUri()).isEqualTo("http://testclient.lyft.com");
        assertThat(certs.get(0).getSubject()).isEqualTo("/C=US/ST=CA/L=San Francisco/OU=Lyft/CN=Test Client");
        assertThat(certs.get(0).getSanDns()).containsExactly("lyft.com", "www.lyft.com");
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

    @Test
    public void quotedKeyThrows() {
        String header = "\"By\"=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "Subject=\"/C=US/ST=CA/L=\\\"San Francisco\"/OU=Lyft/CN=Test Client\";SAN=http://testclient.lyft.com";

        assertThatThrownBy(() -> XfccParser.parse(header)).isInstanceOf(RuntimeException.class);
    }
}
