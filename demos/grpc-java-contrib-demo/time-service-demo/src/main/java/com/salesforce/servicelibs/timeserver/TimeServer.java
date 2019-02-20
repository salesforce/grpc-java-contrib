/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.servicelibs.timeserver;

import com.salesforce.grpc.contrib.spring.GrpcServerHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * A server implementing the TimeService.proto contract using Spring Boot.
 */
// CHECKSTYLE DISABLE FinalClass FOR 2 LINES
@SpringBootApplication
public class TimeServer {
    private final Logger logger = LoggerFactory.getLogger(TimeServer.class);

    public static void main(String[] args) throws Exception {
        SpringApplication.run(TimeServer.class, args);
        Thread.currentThread().join();
    }

    @Bean(initMethod = "start")
    public GrpcServerHost grpcServerHost(@Value("${port}") int port) {
        logger.info("Listening for gRPC on port " + port);
        return new GrpcServerHost(port);
    }

    @Bean
    public TimeServiceImpl timeService() {
        return new TimeServiceImpl();
    }
}
