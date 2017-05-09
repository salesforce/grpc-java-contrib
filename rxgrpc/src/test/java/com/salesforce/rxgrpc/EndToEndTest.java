/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import com.google.protobuf.Empty;
import com.salesforce.grpc.contrib.MoreTimestamps;
import com.salesforce.jprotoc.GreeterGrpc;
import com.salesforce.jprotoc.HelloRequest;
import com.salesforce.jprotoc.HelloResponse;
import com.salesforce.jprotoc.TimeResponse;
import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

public class EndToEndTest {
    @Test
    public void EndToEndTest() throws IOException {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }

            @Override
            public void sayTime(Empty request, StreamObserver<TimeResponse> responseObserver) {
                responseObserver.onNext(TimeResponse.newBuilder().setTime(MoreTimestamps.fromInstantUtc(Instant.now())).build());
                responseObserver.onCompleted();
            }
        };

        Server server = InProcessServerBuilder.forName("e2e").addService(svc).build().start();

        Channel channel = InProcessChannelBuilder.forName("e2e").usePlaintext(true).build();
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

        String hello = stub.sayHello(HelloRequest.newBuilder().setName("rxjava").build()).getMessage();

        assertThat(hello).isEqualTo("Hello rxjava");
    }
}
