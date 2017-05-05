/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.servicelibs.timeclient;

import com.google.protobuf.Empty;
import com.salesforce.grpc.contrib.MoreTimestamps;
import com.salesforce.servicelibs.TimeReply;
import com.salesforce.servicelibs.TimeServiceGrpc8;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches the current time from a Time Server implementing TimeService.proto.
 */
public final class TimeClient {
    private TimeClient() { }

    public static void main(String[] args) throws Exception {
        // Open a channel to the server
        String authority = args[0];
        Channel channel = ManagedChannelBuilder
                .forTarget("mesh://timeService")
                .usePlaintext(true)
                .build();

        // Create a CompletableFuture-based stub
        TimeServiceGrpc8.TimeServiceCompletableFutureStub stub = TimeServiceGrpc8.newCompletableFutureStub(channel);

        // Call the service
        CompletableFuture<TimeReply> completableFuture = stub.getTime(Empty.getDefaultInstance());
        TimeReply timeReply = completableFuture.get();

        // Convert to JDK8 types
        Instant now = MoreTimestamps.toInstantUtc(timeReply.getTime());
        System.out.println("The time is " + now);
    }
}
