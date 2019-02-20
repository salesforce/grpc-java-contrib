/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.grpc.contrib.spring;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * {@code GrpcService} is an annotation that is used to mark a gRPC service implementation for automatic inclusion in
 * your server.
 */
@Service
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlsoAGrpcService {

}
