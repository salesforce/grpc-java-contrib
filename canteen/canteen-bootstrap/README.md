# Canteen Bootstrap

This module contains the golang bootstrap shim used to invoke `java -jar`. The shim tries to be transparent. It passes
all command line arguments to the child process, so you can use it as if it were the Java process itself.

Once the bootstrap is running, stdin, stdout, and stderr are proxied to the child process. When the child process
exits, the bootstrap assumes the child's exit code. At the moment, the bootstrap does not proxy signals between the
shell and the child process.

The bootstrap is cross-compiled for 64-bit Linux, MacOS, and Windows as part of the Maven build and attached as
additional artifacts with classifiers compatible with the [os-maven-plugin](https://github.com/trustin/os-maven-plugin).

## Improvements

* Propagate signals to and from child process
* More platforms
* More architectures