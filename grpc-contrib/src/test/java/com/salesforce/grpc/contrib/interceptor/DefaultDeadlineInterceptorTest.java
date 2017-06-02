/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import com.salesforce.jprotoc.GreeterGrpc;
import com.salesforce.jprotoc.HelloRequest;
import com.salesforce.jprotoc.HelloResponse;
import io.grpc.ClientInterceptors;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.stub.StreamObserver;
import org.junit.Rule;
import io.grpc.testing.GrpcServerRule;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("ALL")
public class DefaultDeadlineInterceptorTest {
    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Test
    public void interceptorShouldAddDeadlineWhenAbsent() {
        TestGreeterService svc = new TestGreeterService();
        grpcServerRule.getServiceRegistry().addService(svc);

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(
                ClientInterceptors.intercept(grpcServerRule.getChannel(),
                        new DefaultDeadlineInterceptor(1, TimeUnit.SECONDS)));

        stub.sayHello(HelloRequest.getDefaultInstance());

        assertThat(svc.foundDeadline).isNotNull();
        assertThat(svc.foundDeadline.timeRemaining(TimeUnit.MILLISECONDS)).isGreaterThan(0);
        assertThat(svc.foundDeadline.timeRemaining(TimeUnit.MILLISECONDS)).isLessThan(1000);
    }

    @Test
    public void interceptorShouldNotModifyExplicitDeadline() {
        TestGreeterService svc = new TestGreeterService();
        grpcServerRule.getServiceRegistry().addService(svc);

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(
                ClientInterceptors.intercept(grpcServerRule.getChannel(),
                        new DefaultDeadlineInterceptor(1, TimeUnit.SECONDS)));

        stub.withDeadlineAfter(10, TimeUnit.SECONDS)
                .sayHello(HelloRequest.getDefaultInstance());

        assertThat(svc.foundDeadline).isNotNull();
        assertThat(svc.foundDeadline.timeRemaining(TimeUnit.MILLISECONDS)).isGreaterThan(9000);
    }

    @Test
    public void interceptorShouldNotModifyContextDeadline() throws Exception {
        TestGreeterService svc = new TestGreeterService();
        grpcServerRule.getServiceRegistry().addService(svc);

        Context.current()
                .withDeadlineAfter(10, TimeUnit.SECONDS, Executors.newSingleThreadScheduledExecutor())
                .run(() -> {
                    GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(
                            ClientInterceptors.intercept(grpcServerRule.getChannel(),
                                    new DefaultDeadlineInterceptor(1, TimeUnit.SECONDS)));
                    stub.sayHello(HelloRequest.getDefaultInstance());
                });

        assertThat(svc.foundDeadline).isNotNull();
        assertThat(svc.foundDeadline.timeRemaining(TimeUnit.MILLISECONDS)).isGreaterThan(9000);
    }

    private static class TestGreeterService extends GreeterGrpc.GreeterImplBase {
        private Deadline foundDeadline;

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            foundDeadline = Context.current().getDeadline();
            responseObserver.onNext(HelloResponse.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }
}
