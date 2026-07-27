[![Javadocs](https://javadoc.io/badge/com.salesforce.servicelibs/grpc-testing-contrib.svg)](https://javadoc.io/doc/com.salesforce.servicelibs/grpc-testing-contrib)
[![Maven Central](https://img.shields.io/maven-central/v/com.salesforce.servicelibs/grpc-testing-contrib)](https://central.sonatype.com/artifact/com.salesforce.servicelibs/grpc-testing-contrib)

Classes
==============
* *NettyGrpcServerRule* - A jUnit test @Rule like `GrpcServerRule`, but uses the Netty transport instead of the InProc transport.
* *GrpcContextRule* - A jUnit test @Rule that fails any test that leaks `io.grpc.Context` information. Useful for testing context-sensitive code.