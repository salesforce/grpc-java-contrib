/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import com.google.common.base.Preconditions;

import io.grpc.Server;

import java.util.concurrent.TimeUnit;

/**
 * {@code Servers} provides static helper methods for working with instances of {@link Server}.
 */
public final class Servers {

    /**
     * Attempt to {@link Server#shutdown()} the {@link Server} gracefully. If the max wait time is exceeded, give up and
     * perform a hard {@link Server#shutdownNow()}.
     *
     * @param server the server to be shutdown
     * @param maxWaitTimeInMillis the max amount of time to wait for graceful shutdown to occur
     * @return the given server
     * @throws InterruptedException if waiting for termination is interrupted
     */
    public static Server shutdownGracefully(Server server, long maxWaitTimeInMillis) throws InterruptedException {
        Preconditions.checkNotNull(server, "server");
        Preconditions.checkArgument(maxWaitTimeInMillis > 0, "maxWaitTimeInMillis must be greater than 0");

        server.shutdown();

        try {
            server.awaitTermination(maxWaitTimeInMillis, TimeUnit.MILLISECONDS);
        } finally {
            server.shutdownNow();
        }

        return server;
    }

    private Servers() { }
}
