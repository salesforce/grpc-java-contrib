[![Javadocs](https://javadoc.io/badge/com.salesforce.servicelibs/grpc-contrib.svg)](https://javadoc.io/doc/com.salesforce.servicelibs/grpc-contrib)
[![Maven Central](https://img.shields.io/maven-central/v/com.salesforce.servicelibs/grpc-contrib)](https://central.sonatype.com/artifact/com.salesforce.servicelibs/grpc-contrib)

Classes
==============
* *FallbackResolver* - Allows the use of multiple gRPC `NameResolver`s.
* *FutureChain* - A fluent interface for chaining `ListenableFuture` operations together.
* *LambdaStreamObserver* - Create gRPC `StreamObserver`s using JDK8 lambda syntax for brevity.
* *MoreDurations* - JDK8 adapters for the protobuf `Duration` type.
* *MoreFutures* - JDK8 adapters for `ListenableFuture`.
* *MoreMetadata* - JSON and Protobuf marshallers for HTTP/2 request headers.
* *MoreTimestamps* - JDK8 adapters for the protobuf `Timestamp` type.
* *Servers* - Additional helper methods for working with `Server` instances.
* *StaticResolver* - a gRPC `NameResolver` that always resolves to the same address.
* *Statuses* - Utility methods for working with gRPC `Status` objects.

Subpackages
===============
* *context* - Implements an ambient context that is transparently passed from service to service.
* *instancemode* - Adds per-call and per-session service instantiation modes to gRPC.
* *interceptor* - Useful client and server interceptor implementations.
