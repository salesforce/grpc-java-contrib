/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.rxgrpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UnimplementedMethodIntegrationTest {
    private static Server server;
    private static ManagedChannel channel;

    @BeforeClass
    public static void setupServer() throws Exception {
        GreeterGrpc.GreeterImplBase svc = new RxGreeterGrpc.GreeterImplBase() {
            // Don't implement anything
        };

        server = InProcessServerBuilder.forName("e2e").addService(svc).build().start();
        channel = InProcessChannelBuilder.forName("e2e").usePlaintext(true).build();
    }

    @AfterClass
    public static void stopServer() {
        server.shutdown();
        channel.shutdown();
    }

    @Test
    public void unimplementedMethodShouldFail() {
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

        assertThatThrownBy(() -> stub.sayHello(HelloRequest.newBuilder().setName("World").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("UNIMPLEMENTED");
    }
}
