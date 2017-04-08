/*
 * Copyright, 1999-2017, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.salesforce.jprotoc;

import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class CompletableFutureStubTest {
    @Test
    public void AbstractStubFeaturesShouldPropagate() throws Exception {
        com.google.common.base.Preconditions.checkArgument(true);
        Channel channel = InProcessChannelBuilder.forName("ignore").build();
        com.salesforce.jprotoc.GreeterGrpc8.GreeterCompletableFutureStub stub = com.salesforce.jprotoc.GreeterGrpc8
                        .newCompletableFutureStub(channel)
                        .withCompression("bz2")
                        .withMaxInboundMessageSize(42);

        Field innerStubField = com.salesforce.jprotoc.GreeterGrpc8.GreeterCompletableFutureStub.class.getDeclaredField("innerStub");
        innerStubField.setAccessible(true);
        com.salesforce.jprotoc.GreeterGrpc.GreeterFutureStub innerStub = (com.salesforce.jprotoc.GreeterGrpc.GreeterFutureStub) innerStubField.get(stub);

        assertEquals("bz2", stub.getCallOptions().getCompressor());
        assertEquals(new Integer(42), stub.getCallOptions().getMaxInboundMessageSize());

        assertEquals("bz2", innerStub.getCallOptions().getCompressor());
        assertEquals(new Integer(42), innerStub.getCallOptions().getMaxInboundMessageSize());

        assertEquals(stub.getCallOptions().toString(), innerStub.getCallOptions().toString());
    }
}
