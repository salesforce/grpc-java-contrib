/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.sessionid;

import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import com.salesforce.grpc.contrib.interceptor.SessionIdServerInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SessionIdTest {
    @Test
    public void uniqueSessionIdPerChannel() throws Exception {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(HelloResponse.newBuilder().setMessage(SessionIdServerInterceptor.SESSION_ID.get().toString()).build());
                responseObserver.onCompleted();
            }
        };

        Server server = InProcessServerBuilder.forName("uniqueSessionIdPerChannel")
                .addTransportFilter(new ClientSessionTransportFilter())
                .intercept(new SessionIdServerInterceptor())
                .addService(svc)
                .build()
                .start();

        ManagedChannel channel1 = InProcessChannelBuilder.forName("uniqueSessionIdPerChannel")
                .usePlaintext(true)
                .build();
        GreeterGrpc.GreeterBlockingStub stub1 = GreeterGrpc.newBlockingStub(channel1);

        ManagedChannel channel2 = InProcessChannelBuilder.forName("uniqueSessionIdPerChannel")
                .usePlaintext(true)
                .build();
        GreeterGrpc.GreeterBlockingStub stub2 = GreeterGrpc.newBlockingStub(channel2);

        try {
            String sessionId1 = stub1.sayHello(HelloRequest.getDefaultInstance()).getMessage();
            String sessionId2 = stub2.sayHello(HelloRequest.getDefaultInstance()).getMessage();

            assertThat(sessionId1).isNotEqualTo(sessionId2);
        } finally {
            channel1.shutdown();
            channel2.shutdown();
            server.shutdown();
        }
    }

    @Test
    public void interceptorThrowsIfMissingTransportFilter() throws Exception {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(HelloResponse.newBuilder().setMessage(SessionIdServerInterceptor.SESSION_ID.get().toString()).build());
                responseObserver.onCompleted();
            }
        };

        Server server = InProcessServerBuilder.forName("interceptorThrowsIfMissingTransportFilter")
                .intercept(new SessionIdServerInterceptor())
                .addService(svc)
                .build()
                .start();

        ManagedChannel channel = InProcessChannelBuilder.forName("interceptorThrowsIfMissingTransportFilter")
                .usePlaintext(true)
                .build();
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

        try {
            assertThatThrownBy(() -> stub.sayHello(HelloRequest.getDefaultInstance()).getMessage()).isInstanceOf(StatusRuntimeException.class);

        } finally {
            channel.shutdown();
            server.shutdown();
        }
    }
}
