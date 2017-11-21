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
 * x-forwarded-client-cert (XFCC) is a proxy header which indicates certificate information of part or all of the
 * clients or proxies that a request has flowed through, on its way from the client to the server.
 */
public class XForwardedClientCert {
    private String by = "";
    private String hash = "";
    private String san = "";
    private String subject = "";

    void setBy(String by) {
        this.by = by;
    }

    void setHash(String hash) {
        this.hash = hash;
    }

    void setSan(String san) {
        this.san = san;
    }

    void setSubject(String subject) {
        this.subject = subject;
    }

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
        List<String> kvp = new ArrayList<>();
        if (!by.isEmpty()) {
            kvp.add("By=" + enquote(by));
        }
        if (!hash.isEmpty()) {
            kvp.add("Hash=" + enquote(hash));
        }
        if (!san.isEmpty()) {
            kvp.add("SAN=" + enquote(san));
        }
        if (!subject.isEmpty()) {
            kvp.add("Subject=" + enquote(subject));
        }

        return String.join(";", kvp);
    }

    private String enquote(String value) {
        // Escape inner quotes with \"
        value = value.replace("\"", "\\\"");

        // Wrap in quotes if ,;= is present
        if (value.contains(",") || value.contains(";") || value.contains("=")) {
            value = "\"" + value + "\"";
        }

        return value;
    }
}
