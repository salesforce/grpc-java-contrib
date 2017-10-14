/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class StopwatchInterceptorTest {
    @Rule public final GrpcServerRule serverRule = new GrpcServerRule().directExecutor();

    GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
            responseObserver.onCompleted();
        }
    };

    @Test
    public void clientStopwatchWorks() {
        AtomicReference<MethodDescriptor> startDesc = new AtomicReference<>();
        AtomicReference<MethodDescriptor> stopDesc = new AtomicReference<>();
        AtomicReference<Duration> stopDur = new AtomicReference<>();

        //Setup
        serverRule.getServiceRegistry().addService(svc);
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new StopwatchClientInterceptor() {
                    @Override
                    protected void logStart(MethodDescriptor method) {
                        startDesc.set(method);
                    }

                    @Override
                    protected void logStop(MethodDescriptor method, Duration duration) {
                        stopDesc.set(method);
                        stopDur.set(duration);
                    }
                });

        stub.sayHello(HelloRequest.newBuilder().setName("World").build());

        assertThat(startDesc.get().getFullMethodName()).contains("SayHello");
        assertThat(startDesc.get().getFullMethodName()).contains("SayHello");
        assertThat(stopDur.get()).isGreaterThan(Duration.ZERO);
    }

    @Test
    public void serverStopwatchWorks() {
        AtomicReference<MethodDescriptor> startDesc = new AtomicReference<>();
        AtomicReference<MethodDescriptor> stopDesc = new AtomicReference<>();
        AtomicReference<Duration> stopDur = new AtomicReference<>();

        //Setup
        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc,
                new StopwatchServerInterceptor() {
                    @Override
                    protected void logStart(MethodDescriptor method) {
                        startDesc.set(method);
                    }

                    @Override
                    protected void logStop(MethodDescriptor method, Duration duration) {
                        stopDesc.set(method);
                        stopDur.set(duration);
                    }
                }));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        stub.sayHello(HelloRequest.newBuilder().setName("World").build());

        assertThat(startDesc.get().getFullMethodName()).contains("SayHello");
        assertThat(startDesc.get().getFullMethodName()).contains("SayHello");
        assertThat(stopDur.get()).isGreaterThan(Duration.ZERO);
    }
}
