/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.jprotoc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.salesforce.jprotoc.HelloWorldProto.*;

import static org.awaitility.Awaitility.await;

@SuppressWarnings("ALL")
public class CompletableFutureEndToEndTest {

    @Test
    public void serverRunsAndRespondsCorrectly() throws ExecutionException,
            IOException,
            InterruptedException,
            TimeoutException {
        final String name = UUID.randomUUID().toString();

        Server server = ServerBuilder.forPort(9999)
                .addService(new GreeterImpl())
                .build();

        server.start();

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", server.getPort())
                .usePlaintext()
                .build();

        GreeterGrpc8.GreeterCompletableFutureStub stub = GreeterGrpc8.newCompletableFutureStub(channel);

        CompletableFuture<HelloResponse> response = stub.sayHello(HelloRequest.newBuilder().setName(name).build());

        await().atMost(3, TimeUnit.SECONDS).until(() -> response.isDone() && response.get().getMessage().contains(name));

        channel.shutdown();
        channel.awaitTermination(1, TimeUnit.MINUTES);
        channel.shutdownNow();

        server.shutdown();
        server.awaitTermination(1, TimeUnit.MINUTES);
        server.shutdownNow();
    }

    private static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            responseObserver.onNext(
                    HelloResponse.newBuilder()
                            .setMessage("Hello, " + request.getName())
                            .build());

            responseObserver.onCompleted();
        }
    }
}
