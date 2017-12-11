/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.sessionid;

import io.grpc.*;

import java.util.UUID;

/**
 * The {@code SessionIdServerInterceptor} is used in conjunction with {@link SessionIdTransportFilter} to attach a
 * unique {@link UUID} SessionID to all requests from a common client session. The SessionID for a request is stored
 * in the gRPC {@code Session} and is available via the {@link #SESSION_ID} context key.
 *
 * <p>A client session is created each time a client-side {@code ManagedChannel} connects to the server.
 */
public class SessionIdServerInterceptor implements ServerInterceptor {
    /**
     * The gRPC {@code Context.Key} used to access SessionID.
     */
    public static final Context.Key<UUID> SESSION_ID = Context.key("SESSION_ID");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        UUID sessionId = call.getAttributes().get(SessionIdTransportFilter.TRANSPORT_ATTRIBUTES_SESSION_ID);
        if (sessionId != null) {
            return Contexts.interceptCall(Context.current().withValue(SESSION_ID, sessionId), call, headers, next);
        } else {
            throw new IllegalStateException("SessionIdTransportFilter was not registered with " +
                    "ServerBuilder.addTransportFilter(new SessionIdTransportFilter())");
        }
    }
}
