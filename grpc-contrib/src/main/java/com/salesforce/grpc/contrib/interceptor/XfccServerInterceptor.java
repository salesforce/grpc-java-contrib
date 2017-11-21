/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.google.common.annotations.VisibleForTesting;
import com.salesforce.grpc.contrib.XfccBaseListener;
import com.salesforce.grpc.contrib.XfccLexer;
import com.salesforce.grpc.contrib.XfccListener;
import com.salesforce.grpc.contrib.XfccParser;
import io.grpc.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code XfccServerInterceptor} parses the {@code x-forwarded-client-cert} (XFCC) header populated by TLS-terminating
 * reverse proxies. For example, Istio and Linkerd. If present, the parsed XFCC header is appended to the
 * gRPC {@code Context}.
 *
 * @see <a href="https://www.envoyproxy.io/docs/envoy/latest/configuration/http_conn_man/headers.html#config-http-conn-man-headers-x-forwarded-client-cert">Envoy XFCC Header</a>
 * @see <a href="https://github.com/linkerd/linkerd/issues/1153">Linkerd XFCC Header</a>
 */
public class XfccServerInterceptor implements ServerInterceptor {
    /**
     * The metadata key used to access any present {@link XForwardedClientCert} objects.
     */
    public static final Context.Key<List<XForwardedClientCert>> XFCC_CONTEXT_KEY = Context.key("x-forwarded-client-cert");

    private static final Metadata.Key<String> XFCC_METADATA_KEY = Metadata.Key.of("x-forwarded-client-cert", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        try {
            Iterable<String> values = headers.getAll(XFCC_METADATA_KEY);
            if (values != null) {
                List<XForwardedClientCert> xfccs = new ArrayList<>();
                for (String value : values) {
                    xfccs.addAll(parse(value));
                }

                Context xfccContext = Context.current().withValue(XFCC_CONTEXT_KEY, xfccs);
                return Contexts.interceptCall(xfccContext, call, headers, next);
            } else {
                return next.startCall(call, headers);
            }
        } catch (NoClassDefFoundError err) {
            throw new RuntimeException("Likely missing optional dependency on org.antlr:antlr4-runtime:4.7", err);
        }
    }

    @VisibleForTesting
    static List<XForwardedClientCert> parse(String header) {
        List<XForwardedClientCert> clientCerts = new ArrayList<>();

        XfccLexer lexer = new XfccLexer(CharStreams.fromString(header));
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
                    clientCert.by = value.getText();
                }
                if (key.HASH() != null) {
                    clientCert.hash = value.getText();
                }
                if (key.SAN() != null) {
                    clientCert.san = value.getText();
                }
                if (key.SUBJECT() != null) {
                    clientCert.subject = value.getText();
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
    }

    /**
     * x-forwarded-client-cert (XFCC) is a proxy header which indicates certificate information of part or all of the
     * clients or proxies that a request has flowed through, on its way from the client to the server.
     */
    public static class XForwardedClientCert {
        private String by;
        private String hash;
        private String san;
        private String subject;

        /**
         * @return The Subject Alternative Name (SAN) of the current proxy’s certificate.
         */
        public String getBy() {
            return by;
        }

        /**
         * @return The SHA 256 diguest of the current client certificate.
         */
        public String getHash() {
            return hash;
        }

        /**
         * @return The SAN field (URI type) of the current client certificate.
         */
        public String getSan() {
            return san;
        }

        /**
         * @return The Subject field of the current client certificate.
         */
        public String getSubject() {
            return subject;
        }

        @Override
        public String toString() {
            return "XForwardedClientCert{" +
                    "by='" + by + '\'' +
                    ", hash='" + hash + '\'' +
                    ", san='" + san + '\'' +
                    ", subject='" + subject + '\'' +
                    '}';
        }
    }
}
