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
        return shutdownGracefully(server, maxWaitTimeInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Attempt to {@link Server#shutdown()} the {@link Server} gracefully. If the max wait time is exceeded, give up and
     * perform a hard {@link Server#shutdownNow()}.
     *
     * @param server the server to be shutdown
     * @param timeout the max amount of time to wait for graceful shutdown to occur
     * @param unit the time unit denominating the shutdown timeout
     * @return the given server
     * @throws InterruptedException if waiting for termination is interrupted
     */
    public static Server shutdownGracefully(Server server, long timeout, TimeUnit unit) throws InterruptedException {
        Preconditions.checkNotNull(server, "server");
        Preconditions.checkArgument(timeout > 0, "timeout must be greater than 0");
        Preconditions.checkNotNull(unit, "unit");

        server.shutdown();

        try {
            server.awaitTermination(timeout, unit);
        } finally {
            server.shutdownNow();
        }

        return server;
    }

    /**
     * Attempt to {@link Server#shutdown()} the {@link Server} gracefully when the JVM terminates. If the max wait time
     * is exceeded, give up and perform a hard {@link Server#shutdownNow()}.
     *
     * @param server the server to be shutdown
     * @param maxWaitTimeInMillis the max amount of time to wait for graceful shutdown to occur
     * @return the given server
     */
    public static Server shutdownWithJvm(Server server, long maxWaitTimeInMillis) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdownGracefully(server, maxWaitTimeInMillis);
            } catch (InterruptedException ex) {
                // do nothing
            }
        }));

        return server;
    }

    private Servers() { }
}
