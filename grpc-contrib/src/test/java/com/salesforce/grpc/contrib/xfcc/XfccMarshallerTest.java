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

public class XfccMarshallerTest {
    @Test
    public void parseSimpleHeaderWorks() {
        XfccMarshaller marshaller = new XfccMarshaller();

        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "URI=http://testclient.lyft.com";
        List<XForwardedClientCert> certs = marshaller.parseAsciiString(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getBy()).isEqualTo("http://frontend.lyft.com");
        assertThat(certs.get(0).getHash()).isEqualTo("468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688");
        assertThat(certs.get(0).getSanUri()).isEqualTo("http://testclient.lyft.com");
        assertThat(certs.get(0).getSubject()).isEmpty();
    }
    @Test
    public void parseCompoundHeaderWorks() {
        XfccMarshaller marshaller = new XfccMarshaller();

        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;URI=http://testclient.lyft.com," +
                "By=http://backend.lyft.com;Hash=9ba61d6425303443c0748a02dd8de688468ed33be74eee6556d90c0149c1309e;URI=http://frontend.lyft.com";
        List<XForwardedClientCert> certs = marshaller.parseAsciiString(header);

        assertThat(certs.size()).isEqualTo(2);
        assertThat(certs.get(0).getBy()).isEqualTo("http://frontend.lyft.com");
        assertThat(certs.get(1).getBy()).isEqualTo("http://backend.lyft.com");
    }

    @Test
    public void quotedHeaderWorks() {
        XfccMarshaller marshaller = new XfccMarshaller();

        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "Subject=\"/C=US/ST=CA/L=San Francisco/OU=Lyft/CN=Test Client\";URI=http://testclient.lyft.com";
        List<XForwardedClientCert> certs = marshaller.parseAsciiString(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getSubject()).isEqualTo("/C=US/ST=CA/L=San Francisco/OU=Lyft/CN=Test Client");
    }

    @Test
    public void escapedQuotedHeaderWorks() {
        XfccMarshaller marshaller = new XfccMarshaller();

        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "Subject=\"/C=US/ST=CA/L=\\\"San Francisco\\\"/OU=Lyft/CN=Test Client\";URI=http://testclient.lyft.com";
        List<XForwardedClientCert> certs = marshaller.parseAsciiString(header);

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getSubject()).isEqualTo("/C=US/ST=CA/L=\"San Francisco\"/OU=Lyft/CN=Test Client");
    }

    @Test
    public void roundTripSimpleTest() {
        XfccMarshaller marshaller = new XfccMarshaller();

        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "URI=http://testclient.lyft.com";

        List<XForwardedClientCert> certs = marshaller.parseAsciiString(header);
        String serialized = marshaller.toAsciiString(certs);

        assertThat(serialized).isEqualTo(header);
    }

    @Test
    public void roundTripUriAndDnsTest() {

    }

    @Test
    public void roundTripCompoundTest() {
        XfccMarshaller marshaller = new XfccMarshaller();

        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;URI=http://testclient.lyft.com," +
                "By=http://backend.lyft.com;Hash=9ba61d6425303443c0748a02dd8de688468ed33be74eee6556d90c0149c1309e;URI=http://frontend.lyft.com";

        List<XForwardedClientCert> certs = marshaller.parseAsciiString(header);
        String serialized = marshaller.toAsciiString(certs);

        assertThat(serialized).isEqualTo(header);
    }

    @Test
    public void roundTripQuotedTest() {
        XfccMarshaller marshaller = new XfccMarshaller();

        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "URI=http://testclient.lyft.com;Subject=\"/C=US/ST=CA/L=San Francisco/OU=Lyft/CN=Test Client\"";

        List<XForwardedClientCert> certs = marshaller.parseAsciiString(header);
        String serialized = marshaller.toAsciiString(certs);

        assertThat(serialized).isEqualTo(header);
    }

    @Test
    public void roundTripEscapedQuotedTest() {
        XfccMarshaller marshaller = new XfccMarshaller();

        String header = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "URI=http://testclient.lyft.com;Subject=\"/C=US/ST=CA/L=\\\"San Francisco\\\"/OU=Lyft/CN=Test Client\"";

        List<XForwardedClientCert> certs = marshaller.parseAsciiString(header);
        String serialized = marshaller.toAsciiString(certs);

        assertThat(serialized).isEqualTo(header);
    }
}
