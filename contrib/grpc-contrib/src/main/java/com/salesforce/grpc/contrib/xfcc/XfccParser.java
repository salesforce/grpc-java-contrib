/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.xfcc;

import java.util.ArrayList;
import java.util.List;

import static com.salesforce.grpc.contrib.xfcc.XfccQuoteUtil.*;
import static com.salesforce.grpc.contrib.xfcc.XForwardedClientCert.*;

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
            List<String> substrings = quoteAwareSplit(element, ';');
            for (String substring : substrings) {
                List<String> kvp = quoteAwareSplit(substring, '=');
                String key = kvp.get(0).toLowerCase();
                String value = kvp.get(1);

                if (key.equalsIgnoreCase(XFCC_BY)) {
                    cert.setBy(dequote(value));
                }
                if (key.equalsIgnoreCase(XFCC_HASH)) {
                    cert.setHash(dequote(value));
                }
                // Use "SAN:" instead of "URI:" for backward compatibility with previous mesh proxy releases.
                if (key.equalsIgnoreCase(XFCC_SAN) || key.equalsIgnoreCase(XFCC_URI)) {
                    cert.setSanUri(dequote(value));
                }
                if (key.equalsIgnoreCase(XFCC_DNS)) {
                    cert.addSanDns(dequote(value));
                }
                if (key.equalsIgnoreCase(XFCC_SUBJECT)) {
                    cert.setSubject(dequote(value));
                }
            }
            certs.add(cert);
        }

        return certs;
    }
}
