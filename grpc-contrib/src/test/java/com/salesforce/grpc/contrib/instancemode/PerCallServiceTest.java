package com.salesforce.grpc.contrib.instancemode;

import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import com.salesforce.grpc.contrib.instancemode.PerCallService;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PerCallServiceTest {
    @Rule public final GrpcServerRule serverRule = new GrpcServerRule().directExecutor();

    @Test
    public void perCallShouldInstantiateMultipleInstances() {
        AtomicInteger closeCount = new AtomicInteger(0);

        class TestService extends GreeterGrpc.GreeterImplBase implements AutoCloseable {
            public TestService() {}

            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(HelloResponse.newBuilder().setMessage(Integer.toString(System.identityHashCode(this))).build());
                responseObserver.onCompleted();
            }

            @Override
            public void close() throws Exception {
                closeCount.incrementAndGet();
            }
        }

        Supplier<TestService> factory = () -> new TestService();

        serverRule.getServiceRegistry().addService(new PerCallService<TestService>(factory));

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel());

        String oid1 = stub.sayHello(HelloRequest.getDefaultInstance()).getMessage();
        String oid2 = stub.sayHello(HelloRequest.getDefaultInstance()).getMessage();
        String oid3 = stub.sayHello(HelloRequest.getDefaultInstance()).getMessage();

        assertThat(oid1).isNotEqualTo(oid2);
        assertThat(oid1).isNotEqualTo(oid3);
        assertThat(oid2).isNotEqualTo(oid3);

        assertThat(closeCount.get()).isEqualTo(3);
    }

    @Test
    public void perCallShouldFailWrongConstructor() {
        class BadTestService extends GreeterGrpc.GreeterImplBase {
            public BadTestService(int ignored) { }

            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(HelloResponse.newBuilder().setMessage(Integer.toString(System.identityHashCode(this))).build());
                responseObserver.onCompleted();
            }
        }

        assertThatThrownBy(() -> new PerCallService<>(BadTestService.class))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
