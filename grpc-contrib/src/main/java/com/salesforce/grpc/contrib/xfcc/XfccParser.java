/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.xfcc;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code XfccParser} parses the {@code x-forwarded-client-cert} (XFCC) header populated by TLS-terminating
 * reverse proxies.
 *
 * @see <a href="https://www.envoyproxy.io/docs/envoy/latest/configuration/http_conn_man/headers.html#config-http-conn-man-headers-x-forwarded-client-cert">Envoy XFCC Header</a>
 */
final class XfccParser {
    private XfccParser() { }

    /**
     * Given a header string, parse and return a collection of {@link XForwardedClientCert} objects.
     */
    static List<XForwardedClientCert> parse(String header) {
        List<XForwardedClientCert> certs = new ArrayList<>();

        for (String element : quoteAwareSplit(header, ',')) {
            XForwardedClientCert cert = new XForwardedClientCert();
            List<String> kvps = quoteAwareSplit(element, ';');
            for (String kvp : kvps) {
                List<String> l = quoteAwareSplit(kvp, '=');

                if (l.get(0).toLowerCase().equals("by")) {
                    cert.setBy(dequote(l.get(1)));
                }
                if (l.get(0).toLowerCase().equals("hash")) {
                    cert.setHash(dequote(l.get(1)));
                }
                // Use "SAN:" instead of "URI:" for backward compatibility with previous mesh proxy releases.
                if (l.get(0).toLowerCase().equals("san") || l.get(0).toLowerCase().equals("uri")) {
                    cert.setSanUri(dequote(l.get(1)));
                }
                if (l.get(0).toLowerCase().equals("dns")) {
                    cert.addSanDns(dequote(l.get(1)));
                }
                if (l.get(0).toLowerCase().equals("subject")) {
                    cert.setSubject(dequote(l.get(1)));
                }
            }
            certs.add(cert);
        }

        return certs;
    }

    // Break str into individual elements, splitting on delim (not in quotes)
    private static List<String> quoteAwareSplit(String str, char delim) {
        boolean inQuotes = false;
        boolean inEscape = false;

        List<String> elements = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c == delim && !inQuotes) {
                elements.add(buffer.toString());
                buffer = new StringBuilder();
                inEscape = false;
                continue;
            }

            if (c == '"') {
                if (inQuotes) {
                    if (!inEscape) {
                        inQuotes = false;
                    }
                } else {
                    inQuotes = true;

                }
                inEscape = false;
                buffer.append(c);
                continue;
            }

            if (c == '\\') {
                if (!inEscape) {
                    inEscape = true;
                    buffer.append(c);
                    continue;
                }
            }

            // all other characters
            inEscape = false;
            buffer.append(c);
        }

        if (inQuotes) {
            throw new RuntimeException("Quoted string not closed");
        }

        elements.add(buffer.toString());

        return elements;
    }

    // Remove leading and tailing unescaped quotes, remove escaping from escaped internal quotes
    private static String dequote(String str) {
        str = str.replace("\\\"", "\"");
        if (str.startsWith("\"")) {
            str = str.substring(1);
        }
        if (str.endsWith("\"")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }
}
