/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.servicelibs.timeserver;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.salesforce.grpc.contrib.MoreTimestamps;
import com.salesforce.grpc.contrib.spring.GrpcService;
import com.salesforce.servicelibs.TimeReply;
import com.salesforce.servicelibs.TimeServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Implements TimeService.proto.
 */
@GrpcService
public class TimeServiceImpl extends TimeServiceGrpc.TimeServiceImplBase {
    private final Logger logger = LoggerFactory.getLogger(TimeServiceImpl.class);

    @Override
    public void getTime(Empty request, StreamObserver<TimeReply> responseObserver) {
        // JDK8 type
        Instant now = Instant.now();
        logger.info("Reporting the time " + now);

        // Protobuf type
        Timestamp protoNow = MoreTimestamps.fromInstantUtc(now);
        TimeReply reply = TimeReply.newBuilder().setTime(protoNow).build();

        // Respond
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
