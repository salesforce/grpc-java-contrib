/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.xfcc;

import io.grpc.Metadata;

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
        return XfccParser.parse(serialized);
    }
}
