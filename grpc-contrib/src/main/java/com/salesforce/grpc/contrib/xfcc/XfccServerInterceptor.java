/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.xfcc;

import io.grpc.*;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code XfccServerInterceptor} parses the {@code x-forwarded-client-cert} (XFCC) header populated by TLS-terminating
 * reverse proxies. For example: Envoy, Istio, and Linkerd. If present, the parsed XFCC header is appended to the
 * gRPC {@code Context}.
 *
 * @see <a href="https://www.envoyproxy.io/docs/envoy/latest/configuration/http_conn_man/headers.html#config-http-conn-man-headers-x-forwarded-client-cert">Envoy XFCC Header</a>
 * @see <a href="https://github.com/linkerd/linkerd/issues/1153">Linkerd XFCC Header</a>
 */
public final class XfccServerInterceptor implements ServerInterceptor {
    private static final Metadata.Key<List<XForwardedClientCert>> XFCC_METADATA_KEY = Metadata.Key.of("x-forwarded-client-cert", new XfccMarshaller());

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Iterable<List<XForwardedClientCert>> values = headers.getAll(XFCC_METADATA_KEY);
        if (values != null) {
            List<XForwardedClientCert> xfccs = new ArrayList<>();
            for (List<XForwardedClientCert> value : values) {
                xfccs.addAll(value);
            }

            Context xfccContext = Context.current().withValue(XForwardedClientCert.XFCC_CONTEXT_KEY, xfccs);
            return Contexts.interceptCall(xfccContext, call, headers, next);
        } else {
            return next.startCall(call, headers);
        }
    }
}
