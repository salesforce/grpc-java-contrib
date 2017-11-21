/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.xfcc;

import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import io.grpc.Metadata;
import io.grpc.ServerInterceptors;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class XfccServerInterceptorTest {
    @Rule public final GrpcServerRule serverRule = new GrpcServerRule().directExecutor();

    @Test
    public void endToEndTest() {
        AtomicReference<List<XForwardedClientCert>> certs = new AtomicReference<>();

        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                certs.set(XfccServerInterceptor.XFCC_CONTEXT_KEY.get());

                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }
        };

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, new XfccServerInterceptor()));

        String xfcc = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;SAN=http://testclient.lyft.com," +
                "By=http://backend.lyft.com;Hash=9ba61d6425303443c0748a02dd8de688468ed33be74eee6556d90c0149c1309e;SAN=http://frontend.lyft.com";

        Metadata xfccHeader = new Metadata();
        xfccHeader.put(Metadata.Key.of("x-forwarded-client-cert", Metadata.ASCII_STRING_MARSHALLER), xfcc);

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel())
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(xfccHeader));

        stub.sayHello(HelloRequest.newBuilder().setName("World").build());

        assertThat(certs.get().size()).isEqualTo(2);
        assertThat(certs.get().get(0).getBy()).isEqualTo("http://frontend.lyft.com");
        assertThat(certs.get().get(1).getBy()).isEqualTo("http://backend.lyft.com");
    }

    @Test
    public void endToEndTestMultiple() {
        AtomicReference<List<XForwardedClientCert>> certs = new AtomicReference<>();

        GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
                certs.set(XfccServerInterceptor.XFCC_CONTEXT_KEY.get());

                responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
                responseObserver.onCompleted();
            }
        };

        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, new XfccServerInterceptor()));

        String xfcc = "By=http://frontend.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;SAN=http://testclient.lyft.com," +
                "By=http://backend.lyft.com;Hash=9ba61d6425303443c0748a02dd8de688468ed33be74eee6556d90c0149c1309e;SAN=http://frontend.lyft.com";
        String xfcc2 = "By=http://middle.lyft.com;Hash=468ed33be74eee6556d90c0149c1309e9ba61d6425303443c0748a02dd8de688;" +
                "SAN=http://testclient.lyft.com";

        Metadata xfccHeader = new Metadata();
        Metadata.Key<String> key = Metadata.Key.of("x-forwarded-client-cert", Metadata.ASCII_STRING_MARSHALLER);
        xfccHeader.put(key, xfcc);
        xfccHeader.put(key, xfcc2);

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(serverRule.getChannel())
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(xfccHeader));

        stub.sayHello(HelloRequest.newBuilder().setName("World").build());

        assertThat(certs.get().size()).isEqualTo(3);
        assertThat(certs.get().get(0).getBy()).isEqualTo("http://frontend.lyft.com");
        assertThat(certs.get().get(1).getBy()).isEqualTo("http://backend.lyft.com");
        assertThat(certs.get().get(2).getBy()).isEqualTo("http://middle.lyft.com");
    }
}
