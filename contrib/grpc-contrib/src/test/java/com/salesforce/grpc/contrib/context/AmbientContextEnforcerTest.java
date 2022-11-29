/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.context;

import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import com.salesforce.grpc.contrib.Statuses;
import io.grpc.*;
import io.grpc.Status.Code;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AmbientContextEnforcerTest {
    @Rule public final GrpcServerRule serverRule = new GrpcServerRule();

    GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
            responseObserver.onCompleted();
        }
    };

    @Test
    public void clientEnforcerPasses() {
        // Plumbing
        serverRule.getServiceRegistry().addService(svc);
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new AmbientContextEnforcerClientInterceptor("ctx-test"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            Metadata.Key<String> k = Metadata.Key.of("ctx-test", Metadata.ASCII_STRING_MARSHALLER);
            AmbientContext.current().put(k, "v");

            stub.sayHello(HelloRequest.newBuilder().setName("World").build());
        });
    }

    @Test
    public void clientEnforcerFailsMissing() {
        // Plumbing
        serverRule.getServiceRegistry().addService(svc);
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new AmbientContextEnforcerClientInterceptor("ctx-test"));

        // Test
        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOf(AmbientContextEnforcerClientInterceptor.MissingAmbientContextException.class);
    }

    @Test
    public void clientEnforcerFailsIncomplete() {
        // Plumbing
        serverRule.getServiceRegistry().addService(svc);
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new AmbientContextEnforcerClientInterceptor("ctx-test"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                    .isInstanceOf(AmbientContextEnforcerClientInterceptor.MissingAmbientContextException.class)
                    .hasMessageContaining("ctx-test");
        });
    }

    @Test
    public void serverEnforcerPasses() {
        // Plumbing
        serverRule.getServiceRegistry().addService(ServerInterceptors.interceptForward(svc,
                new AmbientContextServerInterceptor("ctx-"),
                new AmbientContextEnforcerServerInterceptor("ctx-test")
        ));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            Metadata.Key<String> k = Metadata.Key.of("ctx-test", Metadata.ASCII_STRING_MARSHALLER);
            AmbientContext.current().put(k, "v");

            stub.sayHello(HelloRequest.newBuilder().setName("World").build());
        });
    }

    @Test
    public void serverEnforcerFailsMissing() {
        // Plumbing
        serverRule.getServiceRegistry().addService(ServerInterceptors.interceptForward(svc,
                new AmbientContextServerInterceptor("ctx-"),
                new AmbientContextEnforcerServerInterceptor("ctx-test")
        ));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Test
        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOfAny(StatusRuntimeException.class)
                .has(new Condition<Throwable>() {
                    @Override
                    public boolean matches(Throwable throwable) {
                        return Statuses.hasStatusCode(throwable, Code.FAILED_PRECONDITION);
                    }
                });
    }

    @Test
    public void serverEnforcerFailsIncomplete() {
        // Plumbing
        serverRule.getServiceRegistry().addService(ServerInterceptors.interceptForward(svc,
                new AmbientContextServerInterceptor("ctx-"),
                new AmbientContextEnforcerServerInterceptor("ctx-test")
        ));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new AmbientContextClientInterceptor("ctx-"));

        // Test
        AmbientContext.initialize(Context.current()).run(() -> {
            Metadata.Key<String> k = Metadata.Key.of("ctx-unrelated", Metadata.ASCII_STRING_MARSHALLER);
            AmbientContext.current().put(k, "v");

            assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                    .isInstanceOfAny(StatusRuntimeException.class)
                    .hasMessageContaining("ctx-test")
                    .has(new Condition<Throwable>() {
                        @Override
                        public boolean matches(Throwable throwable) {
                            return Statuses.hasStatusCode(throwable, Code.FAILED_PRECONDITION);
                        }
                    });
        });
    }
}
