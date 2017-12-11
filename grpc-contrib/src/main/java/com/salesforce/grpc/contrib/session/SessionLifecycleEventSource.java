/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.session;

/**
 * In interface for classes that emit {@link SessionLifecycleEvent} events.
 */
public interface SessionLifecycleEventSource {
    /**
     * Add a listener for session lifecycle events.
     */
    void addSessionEventListener(SessionLifecycleEventListener listener);

    /**
     * Remove a listener for session lifecycle events.
     */
    void removeSessionEventListener(SessionLifecycleEventListener listener);
}
