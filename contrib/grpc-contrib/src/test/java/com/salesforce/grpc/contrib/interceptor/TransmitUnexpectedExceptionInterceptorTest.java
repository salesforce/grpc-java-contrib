/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TransmitUnexpectedExceptionInterceptorTest {
    @Rule
    public final GrpcServerRule serverRule = new GrpcServerRule();

    @Test
    public void noExceptionDoesNotInterfere() {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }
        };

        ServerInterceptor interceptor = new TransmitUnexpectedExceptionInterceptor();

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, interceptor));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        stub.sayHello(HelloRequest.newBuilder().setName("World").build());
    }

    @Test
    public void exactTypeMatches() {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onError(new ArithmeticException("Divide by zero"));
            }
        };

        ServerInterceptor interceptor = new TransmitUnexpectedExceptionInterceptor().forExactType(ArithmeticException.class);

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, interceptor));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(sre -> ((StatusRuntimeException) sre).getStatus().getCode().equals(Status.INTERNAL.getCode()), "is Status.INTERNAL")
                .hasMessageContaining("Divide by zero");
    }

    @Test
    public void parentTypeMatches() {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onError(new ArithmeticException("Divide by zero"));
            }
        };

        ServerInterceptor interceptor = new TransmitUnexpectedExceptionInterceptor().forParentType(RuntimeException.class);

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, interceptor));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(sre -> ((StatusRuntimeException) sre).getStatus().getCode().equals(Status.INTERNAL.getCode()), "is Status.INTERNAL")
                .hasMessageContaining("Divide by zero");
    }

    @Test
    public void parentTypeMatchesExactly() {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onError(new RuntimeException("Divide by zero"));
            }
        };

        ServerInterceptor interceptor = new TransmitUnexpectedExceptionInterceptor().forParentType(RuntimeException.class);

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, interceptor));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(sre -> ((StatusRuntimeException) sre).getStatus().getCode().equals(Status.INTERNAL.getCode()), "is Status.INTERNAL")
                .hasMessageContaining("Divide by zero");
    }

    @Test
    public void alleMatches() {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onError(new ArithmeticException("Divide by zero"));
            }
        };

        ServerInterceptor interceptor = new TransmitUnexpectedExceptionInterceptor().forAllExceptions();

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, interceptor));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(sre -> ((StatusRuntimeException) sre).getStatus().getCode().equals(Status.INTERNAL.getCode()), "is Status.INTERNAL")
                .hasMessageContaining("Divide by zero");
    }

    @Test
    public void unknownTypeDoesNotMatch() {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onError(new NullPointerException("Bananas!"));
            }
        };

        ServerInterceptor interceptor = new TransmitUnexpectedExceptionInterceptor().forExactType(ArithmeticException.class);

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, interceptor));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(sre -> ((StatusRuntimeException) sre).getStatus().getCode().equals(Status.UNKNOWN.getCode()), "is Status.UNKNOWN")
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    public void unexpectedExceptionCanMatch() {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                throw new ArithmeticException("Divide by zero");
            }
        };

        ServerInterceptor interceptor = new TransmitUnexpectedExceptionInterceptor().forExactType(ArithmeticException.class);

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, interceptor));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(sre -> ((StatusRuntimeException) sre).getStatus().getCode().equals(Status.INTERNAL.getCode()), "is Status.INTERNAL")
                .hasMessageContaining("Divide by zero");
    }

    @Test
    public void unexpectedExceptionCanNotMatch() {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                throw new ArithmeticException("Divide by zero");
            }
        };

        ServerInterceptor interceptor = new TransmitUnexpectedExceptionInterceptor().forExactType(NullPointerException.class);

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, interceptor));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(sre -> ((StatusRuntimeException) sre).getStatus().getCode().equals(Status.UNKNOWN.getCode()), "is Status.UNKNOWN")
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    public void unexpectedExceptionCanMatchStreaming() {
        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHelloStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(HelloResponse.getDefaultInstance());
                responseObserver.onNext(HelloResponse.getDefaultInstance());
                throw new ArithmeticException("Divide by zero");
            }
        };

        ServerInterceptor interceptor = new TransmitUnexpectedExceptionInterceptor().forExactType(ArithmeticException.class);

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, interceptor));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        Iterator<HelloResponse> it = stub.sayHelloStream(HelloRequest.newBuilder().setName("World").build());
        it.next();
        it.next();
        assertThatThrownBy(it::next)
                .isInstanceOf(StatusRuntimeException.class)
                .matches(sre -> ((StatusRuntimeException) sre).getStatus().getCode().equals(Status.INTERNAL.getCode()), "is Status.INTERNAL")
                .hasMessageContaining("Divide by zero");
    }
}
