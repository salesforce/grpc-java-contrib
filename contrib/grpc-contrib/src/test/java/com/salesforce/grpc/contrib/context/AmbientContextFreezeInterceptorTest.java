/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

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

import static org.assertj.core.api.Assertions.assertThat;

public class AmbientContextFreezeInterceptorTest {
    @Rule public final GrpcServerRule serverRule = new GrpcServerRule().directExecutor();

    private class TestService extends GreeterGrpc.GreeterImplBase {
        boolean frozen;

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            frozen = AmbientContext.current().isFrozen();

            responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
            responseObserver.onCompleted();
        }
    };

    @Test
    public void interceptorShouldFreezeContext() {
        TestService svc = new TestService();

        // Plumbing
        serverRule.getServiceRegistry().addService(ServerInterceptors.interceptForward(svc,
                new AmbientContextServerInterceptor("ctx-"),
                new AmbientContextFreezeServerInterceptor()));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Test
        Metadata.Key<String> key = Metadata.Key.of("ctx-k", Metadata.ASCII_STRING_MARSHALLER);
        AmbientContext.initialize(Context.current()).run(() -> {
            AmbientContext.current().put(key, "value");
            stub.sayHello(HelloRequest.newBuilder().setName("World").build());
        });

        assertThat(svc.frozen).isTrue();
    }
}
