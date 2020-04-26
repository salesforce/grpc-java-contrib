/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedList;

import org.junit.Rule;
import org.junit.Test;

import com.salesforce.grpc.contrib.GreeterGrpc;
import com.salesforce.grpc.contrib.HelloRequest;
import com.salesforce.grpc.contrib.HelloResponse;
import com.salesforce.grpc.contrib.interceptor.DebugClientInterceptor.Level;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;

public class DebugClientInterceptorTest {

    @Rule
    public final GrpcServerRule serverRule = new GrpcServerRule().directExecutor();

    private GreeterGrpc.GreeterImplBase svc = new GreeterGrpc.GreeterImplBase() {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
            responseObserver.onNext(HelloResponse.newBuilder().setMessage("Hello " + request.getName()).build());
            responseObserver.onCompleted();
        }
    };

    @Test
    public void clientStopwatchWorks() {
        LinkedList<String> logs = new LinkedList<String>();
        Metadata requestHeaders = new Metadata();
        requestHeaders.put(Metadata.Key.of("request_header", Metadata.ASCII_STRING_MARSHALLER), "request_header_value");
        //Setup
        serverRule.getServiceRegistry().addService(ServerInterceptors.intercept(svc, new ServerInterceptor() {
            @Override
            public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                return next.startCall(new SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void sendHeaders(Metadata responseHeaders) {
                        responseHeaders.put(Metadata.Key.of("response_header", Metadata.ASCII_STRING_MARSHALLER), "response_header_value");
                        super.sendHeaders(responseHeaders);
                    }
                }, headers);
            }
        }));
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc
                .newBlockingStub(serverRule.getChannel())
                .withInterceptors(new DebugClientInterceptor(Level.STATUS, Level.HEADERS, Level.MESSAGE) {
                    @Override
                    protected void log(String message) {
                        logs.add(message);
                    }

                }, MetadataUtils.newAttachHeadersInterceptor(requestHeaders));

        stub.sayHello(HelloRequest.newBuilder().setName("World").build());
        assertThat(logs.poll()).contains("SayHello"); //request method name
        assertThat(logs.poll()).contains(requestHeaders.toString()); //request header value
        assertThat(logs.poll()).contains("World"); //request message
        assertThat(logs.poll()).contains("response_header_value"); //response header
        assertThat(logs.poll()).contains("Hello World"); //response message
        assertThat(logs.poll()).contains("0 OK"); //response status
    }

}
