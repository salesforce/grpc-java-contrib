package com.salesforce.grpc.contrib.instancemode;

import io.grpc.Attributes;
import io.grpc.ServerTransportFilter;

import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

public class PerSessionServerTransportFilter extends ServerTransportFilter {
    static Attributes.Key<UUID> PER_SESSION_KEY = Attributes.Key.of("PER_SESSION_KEY");

    private static Observable terminated = new Observable() {
        @Override
        public void notifyObservers(Object arg) {
            setChanged();
            super.notifyObservers(arg);
        }
    };

    static void subscribeToTerminated(Observer term) {
        terminated.addObserver(term);
    }

    @Override
    public Attributes transportReady(Attributes transportAttrs) {
        return Attributes.newBuilder(transportAttrs).set(PER_SESSION_KEY, UUID.randomUUID()).build();
    }

    @Override
    public void transportTerminated(Attributes transportAttrs) {
        UUID sessionKey = transportAttrs.get(PER_SESSION_KEY);
        if (sessionKey != null) {
            terminated.notifyObservers(sessionKey);
        }
    }
}
