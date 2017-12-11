/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.session;

import java.util.EventObject;
import java.util.UUID;

/**
 * {@code SessionLifecycleEvent}s are fired each time a session begins or ends.
 */
public final class SessionLifecycleEvent extends EventObject {
    private UUID sessionId;

    public SessionLifecycleEvent(Object source, UUID sessionId) {
        super(source);
        this.sessionId = sessionId;
    }

    /**
     * @return  this event's SessionID
     */
    public UUID getSessionId() {
        return sessionId;
    }
}
