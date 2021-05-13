grpc-java-contrib
=================

Useful extensions for using the grpc-java library.

This project is broken down into multiple sub-modules, each solving a different sub-problem.

* [*grpc-contrib*](https://github.com/salesforce/grpc-java-contrib/tree/master/contrib/grpc-contrib) - A collection of utility classes to work with grpc-java.
* [*grpc-testing-contrib*](https://github.com/salesforce/grpc-java-contrib/tree/master/contrib/grpc-testing-contrib) - A collection of utility classes for testing grpc-java.
* [*grpc-spring*](https://github.com/salesforce/grpc-java-contrib/tree/master/contrib/grpc-spring) - Tools for automatically wiring up and starting a grpc service using Spring.
* [*jprotoc*](https://github.com/salesforce/grpc-java-contrib/tree/master/jprotoc) - A framework for building protoc extension plugins in Java.

Demos
=====
A pair of demo applications are in the [`grpc-java-contrib-demo`](https://github.com/salesforce/grpc-java-contrib/tree/master/demos/grpc-java-contrib-demo) directory.

* *time-service-demo* - Hosts a simple gRPC service that reports the current time. Demonstrates grpc-spring service hosting.
* *time-client-demo* - Connects to the time service. Demonstrates `StaticResolver` and jProtoc.

Usage
=====
These libraries are still fairly immature. For now, you will have to clone this repo and build it yourself. Setting
up CI and deploying to Maven Central is still in our future.

See each respective module for documentation on its usage.

Contributing
============
We are happy to talk to you about new features or pull requests. 

* For bugfixes, submit a PR. 
* For new features, create a Github issue first, so we can discuss your plans. Then, submit a PR.
