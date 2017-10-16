package com.salesforce.grpc.contrib.instancemode;

import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import com.salesforce.grpc.contrib.instancemode.PerCallService;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PerCallServiceTest {
    @Rule public final GrpcServerRule serverRule = new GrpcServerRule().directExecutor();

    public static class TestService extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            responseObserver.onNext(HelloResponse.newBuilder().setMessage(Integer.toString(System.identityHashCode(this))).build());
            responseObserver.onCompleted();
        }
    }

    public static class BadTestService extends GreeterGrpc.GreeterImplBase {
        public BadTestService(int ignored) { }

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            responseObserver.onNext(HelloResponse.newBuilder().setMessage(Integer.toString(System.identityHashCode(this))).build());
            responseObserver.onCompleted();
        }
    }

    @Test
    public void perCallShouldInstantiateMultipleInstances() {
        serverRule.getServiceRegistry().addService(new PerCallService<>(TestService.class));

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        String oid1 = stub.sayHello(HelloRequest.getDefaultInstance()).getMessage();
        String oid2 = stub.sayHello(HelloRequest.getDefaultInstance()).getMessage();
        String oid3 = stub.sayHello(HelloRequest.getDefaultInstance()).getMessage();

        assertThat(oid1).isNotEqualTo(oid2);
        assertThat(oid1).isNotEqualTo(oid3);
        assertThat(oid2).isNotEqualTo(oid3);
    }

    @Test
    public void perCallShouldFailWrongConstructor() {
        assertThatThrownBy(() -> new PerCallService<>(BadTestService.class))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
