What is jprotoc?
================
[![Javadocs](https://javadoc.io/badge/com.salesforce.servicelibs/jprotoc.svg)](https://javadoc.io/doc/com.salesforce.servicelibs/jprotoc)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.salesforce.servicelibs/jprotoc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.salesforce.servicelibs/jprotoc)


jprotoc is a framework for building protoc plugins using Java. As a demo, jprotoc includes a Jdk8 generator, that
generates `CompletableFuture`-based client bindings for gRPC.

Creating a new protoc plugin
============================
To create a new proto plugin using jprotoc you need to:

1. Extend the `Generator` base class and implement a `main()` method.
2. Consume your plugin from the Maven protoc plugin.

See `Jdk8Generator.java` for a complete example.

## Implementing a plugin
Protoc plugins need to be in their own stand-alone Maven module due to the way the protoc Maven plugin consumes
protoc plugins. Documentation for protoc's data structures is in 
[plugin.proto](https://github.com/google/protobuf/blob/master/src/google/protobuf/compiler/plugin.proto) and
[descriptor.proto](https://github.com/google/protobuf/blob/master/src/google/protobuf/descriptor.proto).

Create a main class similar to this:
```java
public class MyGenerator extends Generator {
    public static void main(String[] args) {
        if (args.length == 0) {
            // Generate from protoc via stdin
            ProtocPlugin.generate(new MyGenerator());
        } else {
            // Process from a descriptor_dump file via command line arg
            ProtocPlugin.debug(new MyGenerator(), args[0]);
        }
    }

    @Override
    protected List<PluginProtos.CodeGeneratorResponse.Feature> supportedFeatures() {
       return Collections.singletonList(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL);
    }

    @Override
    public Stream<PluginProtos.CodeGeneratorResponse.File> generate(PluginProtos.CodeGeneratorRequest request) 
        throws GeneratorException {
        
        // create a map from proto types to java types
        final ProtoTypeMap protoTypeMap = ProtoTypeMap.of(request.getProtoFileList());
    
        // set context attributes by extracting values from the request
        // use protoTypeMap to translate between proto types and java types
        Context ctx = new Context();
        
        // generate code from an embedded resource Mustache template
        String content = applyTemplate("myTemplate.mustache", context);
    
        // create a new file for protoc to write
        PluginProtos.CodeGeneratorResponse.File file = PluginProtos.CodeGeneratorResponse.File
            .newBuilder()
            .setName("fileName")
            .setContent(content)
            .build();
            
        return Collections.singletonList(file).stream();
    }
    
    private class Context {
        // attributes for use in your code template
    }
}
```

For your plugin, you will most likely want to iterate over the internal proto data structures, creating new Files as
you go. For convenience, jprotoc comes bundled with [Mustache.java](https://github.com/spullara/mustache.java) to make
authoring code templates easy. 

## Using your plugin with Maven
To execute your protoc plugin when Maven compiles, add a `<configuration>` section to the Maven protoc plugin in your
POM file. For more documentation, see the Maven protoc plugin's 
[usage documentation](https://www.xolstice.org/protobuf-maven-plugin/examples/protoc-plugin.html).

```xml
<configuration>
    <protocPlugins>
        <protocPlugin>
            <id>MyGenerator</id>
            <groupId>com.something</groupId>
            <artifactId>myPlugin</artifactId>
            <version>${project.version}</version>
            <mainClass>com.something.MyGenerator</mainClass>
        </protocPlugin>
    </protocPlugins>
</configuration>
```

## Packaging your plugin for native execution

jProtoc plugins can be packaged as native executables using the [`canteen-maven-plugin`](https://github.com/salesforce/grpc-java-contrib/tree/master/canteen).
Canteen repackages jar files so they can be executed from the command line directly without requiring a `java -jar`
invocation.

```xml
<!-- Make the jar self-executing with Canteen -->
<plugin>
    <groupId>com.salesforce.servicelibs</groupId>
    <artifactId>canteen-maven-plugin</artifactId>
    <version>${canteen.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>bootstrap</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Using the Jdk8 Protoc generator
===============================
1. Add the following to your POM:
    ```xml
    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>1.4.1.Final</version>
            </extension>
        </extensions>
        <plugins>
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>0.5.0</version>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:3.0.2:exe:${os.detected.classifier}</protocArtifact>
                    <pluginId>grpc-java</pluginId>
                    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.2.0:exe:${os.detected.classifier}</pluginArtifact>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>compile-custom</goal>
                            <goal>test-compile</goal>
                            <goal>test-compile-custom</goal>
                        </goals>
                        <configuration>
                            <protocPlugins>
                                <protocPlugin>
                                    <id>java8</id>
                                    <groupId>com.salesforce.servicelibs</groupId>
                                    <artifactId>jprotoc</artifactId>
                                    <version>${project.version}</version>
                                    <mainClass>com.salesforce.jprotoc.jdk8.Jdk8Generator</mainClass>
                                </protocPlugin>
                            </protocPlugins>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    ```
2. Run a `mvn build` to generate the java 8 stubs.

3. Reference the java 8 client stubs like this:
    ```java
    MyServiceGrpc8.GreeterCompletableFutureStub stub = MyServiceGrpc8.newCompletableFutureStub(channel);
    ```
    
Implementing Custom Protoc Options
==================================
1. Create a custom option as per https://developers.google.com/protocol-buffers/docs/reference/java-generated#extension.
For example:

    ```
    syntax = "proto3";
    
    package com.example.proto.options;
    
    import "google/protobuf/descriptor.proto";
    
    option java_multiple_files = true;
    option java_outer_classname = "ServerOptionsProto";
    option java_package = "com.example.proto.options";
    
    extend google.protobuf.FileOptions {
        ServerOptions server = 50621;
    }
    
    message ServerOptions {
        // Java classname
        string name = 1;
    }
    ```

2. Run the Protoc generator on the Extension proto.

3. Use `ProtocPlugin.generate(List<Generator> generators, List<GeneratedExtension> extensions)` so
that the option gets registered:

    ```
    class Generator extends com.salesforce.jprotoc.Generator {
      public static void main(String[] args) {
        ProtocPlugin.generate([new Generator()], [com.example.proto.options.ServerOptionsProto.server]);
      }
      
      // ?
    }
    ```

Debugging protoc plugins
========================
jProtoc includes a dump plugin, which writes the protoc descriptor set to a file. This file can be passed in as a
command line option to a jProtoc plugin's main method to run the plugin independently of protoc.

1. Run the dump plugin as part of your build.

```xml
<configuration>
    <protocPlugins>
        <protocPlugin>
            <id>dump</id>
            <groupId>com.salesforce.servicelibs</groupId>
            <artifactId>jprotoc</artifactId>
            <version>${project.version}</version>
            <mainClass>com.salesforce.jprotoc.dump.DumpGenerator</mainClass>
        </protocPlugin>
    </protocPlugins>
</configuration>
```

The dump plugin will emit `descriptor_dump` and `descriptor_dump.json` as output. Use the json file to better 
understand how protoc structures the input to your plugin. Use the binary file as an input to your plugin for 
debugging.

2. Run your plugin using `descriptor_dump` as input.

```bash
> java -jar myPlugin.jar path/to/descriptor_dump
```

Or, use your IDE to debug, passing in a command line option.