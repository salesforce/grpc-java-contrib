/*
 *  Copyright (c) 2018, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.testing.contrib;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("Duplicates")
public class NettyGrpcServerRuleTest {
    @Rule public final NettyGrpcServerRule grpcServerRule = new NettyGrpcServerRule();

    @Test
    public void serverAndChannelAreStarted_withoutDirectExecutor() {
        assertThat(grpcServerRule.getServer().isShutdown()).isFalse();
        assertThat(grpcServerRule.getServer().isTerminated()).isFalse();

        assertThat(grpcServerRule.getChannel().isShutdown()).isFalse();
        assertThat(grpcServerRule.getChannel().isTerminated()).isFalse();

        assertThat(grpcServerRule.getPort()).isNotZero();
        assertThat(grpcServerRule.getServiceRegistry()).isNotNull();
    }

    @Test
    public void serverAllowsServicesToBeAddedViaServiceRegistry_withoutDirectExecutor() {
        TestServiceImpl testService = new TestServiceImpl();

        grpcServerRule.getServiceRegistry().addService(testService);

        SimpleServiceGrpc.SimpleServiceBlockingStub stub =
                SimpleServiceGrpc.newBlockingStub(grpcServerRule.getChannel());

        SimpleRequest request1 = SimpleRequest.getDefaultInstance();

        SimpleRequest request2 = SimpleRequest.newBuilder().build();

        stub.unaryRpc(request1);
        stub.unaryRpc(request2);

        assertThat(testService.unaryCallRequests).containsExactly(request1, request2);
    }

    @Test
    public void serviceIsNotRunOnSameThreadAsTest_withoutDirectExecutor() {
        TestServiceImpl testService = new TestServiceImpl();

        grpcServerRule.getServiceRegistry().addService(testService);

        SimpleServiceGrpc.SimpleServiceBlockingStub stub =
                SimpleServiceGrpc.newBlockingStub(grpcServerRule.getChannel());

        stub.serverStreamingRpc(SimpleRequest.getDefaultInstance());

        assertThat(testService.lastServerStreamingRpcThread).isNotEqualTo(Thread.currentThread());
    }

    @Test
    public void serverAndChannelAreShutdownAfterRule() throws Throwable {
        NettyGrpcServerRule grpcServerRule = new NettyGrpcServerRule();

        // Before the rule has been executed, all of its resources should be null.
        assertThat(grpcServerRule.getChannel()).isNull();
        assertThat(grpcServerRule.getServer()).isNull();
        assertThat(grpcServerRule.getPort()).isZero();
        assertThat(grpcServerRule.getServiceRegistry()).isNull();

        // The TestStatement stores the channel and server instances so that we can inspect them after
        // the rule cleans up.
        TestStatement statement = new TestStatement(grpcServerRule);

        grpcServerRule.apply(statement, null).evaluate();

        // Ensure that the stored channel and server instances were shut down.
        assertThat(statement.channel.isShutdown()).isTrue();
        assertThat(statement.server.isShutdown()).isTrue();

        // All references to the resources that we created should be set to null.
        assertThat(grpcServerRule.getChannel()).isNull();
        assertThat(grpcServerRule.getServer()).isNull();
        assertThat(grpcServerRule.getPort()).isZero();
        assertThat(grpcServerRule.getServiceRegistry()).isNull();
    }

    private static class TestStatement extends Statement {

        private final NettyGrpcServerRule grpcServerRule;

        private ManagedChannel channel;
        private Server server;

        private TestStatement(NettyGrpcServerRule grpcServerRule) {
            this.grpcServerRule = grpcServerRule;
        }

        @Override
        public void evaluate() throws Throwable {
            channel = grpcServerRule.getChannel();
            server = grpcServerRule.getServer();
        }
    }

    private static class TestServiceImpl extends SimpleServiceGrpc.SimpleServiceImplBase {

        private final Collection<SimpleRequest> unaryCallRequests =
                new ConcurrentLinkedQueue<SimpleRequest>();

        private volatile Thread lastServerStreamingRpcThread;

        @Override
        public void serverStreamingRpc(
                SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {

            lastServerStreamingRpcThread = Thread.currentThread();

            responseObserver.onNext(SimpleResponse.getDefaultInstance());

            responseObserver.onCompleted();
        }

        @Override
        public void unaryRpc(
                SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {

            unaryCallRequests.add(request);

            responseObserver.onNext(SimpleResponse.getDefaultInstance());

            responseObserver.onCompleted();
        }
    }
}
