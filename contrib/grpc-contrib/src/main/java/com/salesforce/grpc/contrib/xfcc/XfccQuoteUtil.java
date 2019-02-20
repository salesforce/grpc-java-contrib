/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.grpc.contrib.xfcc;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for quote escaping in XFCC headers.
 */
final class XfccQuoteUtil {
    private XfccQuoteUtil() { }

    /**
     * Break str into individual elements, splitting on delim (not in quotes).
     */
    static List<String> quoteAwareSplit(String str, char delim) {
        boolean inQuotes = false;
        boolean inEscape = false;

        List<String> elements = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c == delim && !inQuotes) {
                elements.add(buffer.toString());
                buffer.setLength(0); // clear
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

    /**
     * Add escaping around double quote characters; wrap with quotes if special characters are present.
     */
    static String enquote(String value) {
        // Escape inner quotes with \"
        value = value.replace("\"", "\\\"");

        // Wrap in quotes if ,;= is present
        if (value.contains(",") || value.contains(";") || value.contains("=")) {
            value = "\"" + value + "\"";
        }

        return value;
    }

    /**
     * Remove leading and tailing unescaped quotes; remove escaping from escaped internal quotes.
     */
    static String dequote(String str) {
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
