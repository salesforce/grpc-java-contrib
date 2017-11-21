/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.xfcc;

import io.grpc.Metadata;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code XfccMarshaller} parses the {@code x-forwarded-client-cert} (XFCC) header populated by TLS-terminating
 * reverse proxies. For example, Istio and Linkerd.
 *
 * @see <a href="https://www.envoyproxy.io/docs/envoy/latest/configuration/http_conn_man/headers.html#config-http-conn-man-headers-x-forwarded-client-cert">Envoy XFCC Header</a>
 * @see <a href="https://github.com/linkerd/linkerd/issues/1153">Linkerd XFCC Header</a>
 */
public class XfccMarshaller implements Metadata.AsciiMarshaller<List<XForwardedClientCert>> {
    @Override
    public String toAsciiString(List<XForwardedClientCert> value) {
        return value.stream().map(XForwardedClientCert::toString).collect(Collectors.joining(","));
    }

    @Override
    public List<XForwardedClientCert> parseAsciiString(String serialized) {
        try {
            List<XForwardedClientCert> clientCerts = new ArrayList<>();

            XfccLexer lexer = new XfccLexer(CharStreams.fromString(serialized));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            XfccParser parser = new XfccParser(tokens);

            XfccParser.HeaderContext headerContext = parser.header();
            ParseTreeWalker walker = new ParseTreeWalker();

            XfccListener listener = new XfccBaseListener() {
                private XForwardedClientCert clientCert;

                @Override
                public void enterElement(XfccParser.ElementContext ctx) {
                    clientCert = new XForwardedClientCert();
                }

                @Override
                public void enterKvp(XfccParser.KvpContext ctx) {
                    XfccParser.KeyContext key = ctx.key();
                    XfccParser.ValueContext value = ctx.value();

                    if (key.BY() != null) {
                        clientCert.setBy(value.getText());
                    }
                    if (key.HASH() != null) {
                        clientCert.setHash(value.getText());
                    }
                    if (key.SAN() != null) {
                        clientCert.setSan(value.getText());
                    }
                    if (key.SUBJECT() != null) {
                        clientCert.setSubject(value.getText());
                    }
                }

                @Override
                public void exitElement(XfccParser.ElementContext ctx) {
                    clientCerts.add(clientCert);
                    clientCert = null;
                }
            };

            walker.walk(listener, headerContext);
            return clientCerts;
        } catch (NoClassDefFoundError err) {
            throw new RuntimeException("Likely missing optional dependency on org.antlr:antlr4-runtime:4.7", err);
        }
    }
}
