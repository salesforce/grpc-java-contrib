Using grpc-spring
=================
1. Tag your gRPC service implementation with the `@GrpcService` annotation.
   ```java
   @GrpcService
   private static class MyServiceImpl extends MyServiceGrpc.MyServiceImplBase {
       // implement service operations
   }
   ```
   
2. Set up a Spring configuration section for your gRPC services and the `GrpcServerHost`.
   ```java
   @Configuration
   static class ServiceConfiguration {
       @Bean
       public MyServiceImpl myService() {
           return new MyServiceImpl();
       }
    
       // More services here

       @Bean
       public GrpcServerHost serverHost() throws IOException {
           return new GrpcServerHost(9999);
       }
   }
   ```
   
3. Start the server.
   ```java
   @Autowired
   private GrpcServerHost serverHost;

   ...
   serverHost.start();

   ```