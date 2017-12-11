/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.sessionid;

import java.util.EventListener;

/**
 * An interface for listening to {@link SessionLifecycleEvent}s.
 */
public interface SessionLifecycleEventListener extends EventListener {
    /**
     * Called when a gRPC session begins.
     */
    void sessionStart(SessionLifecycleEvent sessionLifecycleEvent);

    /**
     * Called when a gRPC session ends.
     */
    void sessionEnd(SessionLifecycleEvent sessionLifecycleEvent);
}
