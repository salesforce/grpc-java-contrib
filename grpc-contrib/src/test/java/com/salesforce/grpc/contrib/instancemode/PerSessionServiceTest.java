package com.salesforce.grpc.contrib.instancemode;

import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import com.salesforce.grpc.contrib.interceptor.SessionIdServerInterceptor;
import com.salesforce.grpc.contrib.session.ClientSessionTransportFilter;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PerSessionServiceTest {
    @Test
    public void perSessionShouldInstantiateOneInstancePerSession() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);

        class TestService extends GreeterGrpc.GreeterImplBase implements AutoCloseable {
            public TestService() {}

            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                if (SessionIdServerInterceptor.SESSION_ID.get() == null) {
                    responseObserver.onError(new Exception("Missing SESSION_ID"));
                } else {
                    responseObserver.onNext(HelloResponse.newBuilder().setMessage(Integer.toString(System.identityHashCode(this))).build());
                    responseObserver.onCompleted();
                }
            }

            @Override
            public void close() throws Exception {
                closeCount.incrementAndGet();
            }
        }

        ClientSessionTransportFilter tf = new ClientSessionTransportFilter();
        Server server = InProcessServerBuilder.forName("perSessionShouldInstantiateOneInstancePerSession")
                .addTransportFilter(tf)
                .intercept(new SessionIdServerInterceptor())
                .addService(new PerSessionService<>(() -> new TestService(), tf))
                .build()
                .start();

        ManagedChannel channel1 = InProcessChannelBuilder.forName("perSessionShouldInstantiateOneInstancePerSession")
                .usePlaintext(true)
                .build();
        GreeterGrpc.GreeterBlockingStub stub1 = GreeterGrpc.newBlockingStub(channel1);

        ManagedChannel channel2 = InProcessChannelBuilder.forName("perSessionShouldInstantiateOneInstancePerSession")
                .usePlaintext(true)
                .build();
        GreeterGrpc.GreeterBlockingStub stub2 = GreeterGrpc.newBlockingStub(channel2);

        ManagedChannel channel3 = InProcessChannelBuilder.forName("perSessionShouldInstantiateOneInstancePerSession")
                .usePlaintext(true)
                .build();
        GreeterGrpc.GreeterBlockingStub stub3 = GreeterGrpc.newBlockingStub(channel3);

        try {
            String oid11 = stub1.sayHello(HelloRequest.getDefaultInstance()).getMessage();
            String oid21 = stub2.sayHello(HelloRequest.getDefaultInstance()).getMessage();
            String oid31 = stub3.sayHello(HelloRequest.getDefaultInstance()).getMessage();

            String oid12 = stub1.sayHello(HelloRequest.getDefaultInstance()).getMessage();
            String oid22 = stub2.sayHello(HelloRequest.getDefaultInstance()).getMessage();
            String oid32 = stub3.sayHello(HelloRequest.getDefaultInstance()).getMessage();

            assertThat(oid11).isEqualTo(oid12);
            assertThat(oid21).isEqualTo(oid22);
            assertThat(oid31).isEqualTo(oid32);

            assertThat(oid11).isNotEqualTo(oid21);
            assertThat(oid21).isNotEqualTo(oid31);
            assertThat(oid31).isNotEqualTo(oid11);
        } finally {
            channel1.shutdown();
            server.shutdown();
            channel2.shutdown();
            channel3.shutdown();
        }

        assertThat(closeCount.get()).isEqualTo(3);
    }

    @Test
    public void perSessionShouldFailWrongConstructor() {
        class BadTestService extends GreeterGrpc.GreeterImplBase {
            public BadTestService(int ignored) { }
        }

        ClientSessionTransportFilter tf = new ClientSessionTransportFilter();
        assertThatThrownBy(() -> new PerSessionService<>(BadTestService.class, tf))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void perSessionShouldFailMissingTransportFilter() throws Exception {
        class TestService extends GreeterGrpc.GreeterImplBase {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                responseObserver.onNext(HelloResponse.newBuilder().setMessage(Integer.toString(System.identityHashCode(this))).build());
                responseObserver.onCompleted();
            }
        }

        ClientSessionTransportFilter tf = new ClientSessionTransportFilter();
        Server server = InProcessServerBuilder.forName("perSessionShouldInstantiateOneInstancePerSession")
                .addService(new PerSessionService<>(() -> new TestService(), tf))
                .build()
                .start();

        ManagedChannel channel = InProcessChannelBuilder.forName("perSessionShouldInstantiateOneInstancePerSession")
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
