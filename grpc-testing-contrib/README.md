[![Javadocs](https://javadoc.io/badge/com.salesforce.servicelibs/grpc-contrib.svg)](https://javadoc.io/doc/com.salesforce.servicelibs/grpc-contrib)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.salesforce.servicelibs/grpc-contrib/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.salesforce.servicelibs/grpc-contrib)

Classes
==============
* *NettyGrpcServerRule* - A jUnit test @Rule like `GrpcServerRule`, but uses the Netty transport instead of the InProc transport.
* *GrpcContext* - A jUnit test @Rule that fails any test that leaks `io.grpc.Context` information. Useful for testing context-sensitive code.