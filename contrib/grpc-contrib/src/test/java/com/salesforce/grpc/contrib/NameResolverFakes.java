/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib;

import io.grpc.NameResolver;
import io.grpc.SynchronizationContext;
import io.grpc.internal.GrpcUtil;

import java.util.Map;

public class NameResolverFakes {
    final SynchronizationContext fakeSyncContext = new SynchronizationContext(new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            e.printStackTrace();
        }
    });
    final NameResolver.ServiceConfigParser fakeParser =new NameResolver.ServiceConfigParser() {
        @Override
        public NameResolver.ConfigOrError parseServiceConfig(Map<String, ?> rawServiceConfig) {
            return NameResolver.ConfigOrError.fromConfig(new Object());
        }
    };

    final NameResolver.Args fakeArgs = NameResolver.Args.newBuilder()
            .setDefaultPort(GrpcUtil.DEFAULT_PORT_PLAINTEXT)
            .setProxyDetector(GrpcUtil.DEFAULT_PROXY_DETECTOR)
            .setSynchronizationContext(fakeSyncContext)
            .setServiceConfigParser(fakeParser)
            .build();
}
