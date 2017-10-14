/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import com.google.common.io.BaseEncoding;
import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class BinaryAmbientContextTest {
    @Rule public final GrpcServerRule serverRule = new GrpcServerRule();

    @Test
    public void binaryContextValueTransfers() throws Exception {
        Metadata.Key<byte[]> ctxKey = Metadata.Key.of(
                "ctx-context-key" + Metadata.BINARY_HEADER_SUFFIX,
                Metadata.BINARY_BYTE_MARSHALLER);
        byte[] expectedCtxValue = BaseEncoding.base16().decode("DEADBEEF");
        AtomicReference<byte[]> ctxValue = new AtomicReference<>();

        // Service
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                ctxValue.set(AmbientContext.current().get(ctxKey));
                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }
        };

        // Plumbing
        serverRule.getServiceRegistry().addService(ServerInterceptors
                .intercept(svc, new AmbientContextServerInterceptor("ctx-")));

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            AmbientContext.current().put(ctxKey, expectedCtxValue);
            stub.sayHello(HelloRequest.newBuilder().setName("world").build());
        });

        assertThat(ctxValue.get()).containsExactly(expectedCtxValue);
    }
}
