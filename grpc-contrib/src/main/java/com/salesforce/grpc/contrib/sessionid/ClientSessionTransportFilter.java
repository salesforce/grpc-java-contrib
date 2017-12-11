/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.sessionid;

import io.grpc.Attributes;
import io.grpc.ServerTransportFilter;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code ClientSessionTransportFilter} is a gRPC {@code TransportFilter} that attaches a unique SessionID to all requests
 * from a common client session. A client session is created each time a client-side {@code ManagedChannel} connects
 * to the server. For easy access to the SessionID from service implementations, add a
 * {@link com.salesforce.grpc.contrib.interceptor.SessionIdServerInterceptor} to the request chain.
 *
 * <p>The {@code ClientSessionTransportFilter} is installed using {@code "ServerBuilder.addTransportFilter(new ClientSessionTransportFilter())}.
 * When installed, {@code ClientSessionTransportFilter} attaches the SessionID to gRPC's {@code ServerCall} transport
 * attributes.
 */
public class ClientSessionTransportFilter extends ServerTransportFilter implements SessionLifecycleEventSource {
    /**
     * The key used to retrieve a SessionID from gRPC's {@code ServerCall} transport attributes.
     */
    public static final Attributes.Key<UUID> TRANSPORT_ATTRIBUTES_SESSION_ID =
            Attributes.Key.of("TRANSPORT_ATTRIBUTES_SESSION_ID");

    private final Set<SessionLifecycleEventListener> sessionLifecycleEventListeners = new CopyOnWriteArraySet<>();

    @Override
    public void addSessionEventListener(SessionLifecycleEventListener listener) {
        checkNotNull(listener, "listener");
        sessionLifecycleEventListeners.add(listener);
    }

    @Override
    public void removeSessionEventListener(SessionLifecycleEventListener listener) {
        checkNotNull(listener, "listener");
        sessionLifecycleEventListeners.remove(listener);
    }

    @Override
    public Attributes transportReady(Attributes transportAttrs) {
        checkNotNull(transportAttrs, "transportAttrs");

        UUID sessionId = UUID.randomUUID();
        SessionLifecycleEvent event = new SessionLifecycleEvent(this, sessionId);
        sessionLifecycleEventListeners.forEach(listener -> listener.sessionStart(event));

        return Attributes.newBuilder(transportAttrs).set(TRANSPORT_ATTRIBUTES_SESSION_ID, UUID.randomUUID()).build();
    }

    @Override
    public void transportTerminated(Attributes transportAttrs) {
        checkNotNull(transportAttrs, "transportAttrs");

        UUID sessionId = transportAttrs.get(TRANSPORT_ATTRIBUTES_SESSION_ID);
        if (sessionId != null) {
            SessionLifecycleEvent event = new SessionLifecycleEvent(this, sessionId);
            sessionLifecycleEventListeners.forEach(listener -> listener.sessionEnd(event));
        }
    }
}
