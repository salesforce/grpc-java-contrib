/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * The classes in this package are used together to implement the Ambient Context pattern, where context values are
 * transparently marshaled from service to service without user intervention. This pattern is particularly useful for
 * transparently populating system information like Zipkin trace ids and logStop correlation ids on every service call in
 * a call graph.
 *
 * <p>The Ambient Context pattern is implemented in gRPC using the
 * {@link com.salesforce.grpc.contrib.context.AmbientContextServerInterceptor} and
 * {@link com.salesforce.grpc.contrib.context.AmbientContextClientInterceptor}. Together these interceptors marshall
 * context headers from the gRPC {@code Context} into outbound request {@code Metadata}, and from inbound request
 * {@code Metadata} into the gRPC {@code Context}.
 *
 * <p>Within your service and infrastructure code, the {@link com.salesforce.grpc.contrib.context.AmbientContext} class
 * provides utility methods for interacting with the ambient context. {@code AmbientContext}s API is similar to gRPC's
 * {@code Metadata} API, with the addition of the {@code freeze()} and {@code thaw()} operations used to toggle
 * the {@code AmbientContext}'s read-only status.
 *
 * <p>Freezing the ambient context is useful for protecting its contents when transitioning between platform
 * infrastructure code and service implementation code. In many cases, platform infrastructure code and service
 * implementations are written by different groups with different goals. Platform infrastructure implementors typically
 * own inter-service functions like distributed tracing, logStop correlation, and service metrics, typically implemented
 * using the gRPC interceptor chain. For these developers ambient context must be mutable. Service implementors, on
 * the other hand, own the business logic for each service. For these developers it can make sense for the ambient
 * context to be immutable to prevent unnecessary pollution of the ambient context. Freezing and thawing the context
 * allows it to seamlessly transition between mutable and immutable modes as needed by each audience.
 *
 * @see <a href="https://aabs.wordpress.com/2007/12/31/the-ambient-context-design-pattern-in-net/">
 *      The Ambient Context Design Pattern in .NET</a>
 */
package com.salesforce.grpc.contrib.context;