![documentation](https://javadoc.io/badge/com.salesforce.servicelibs/grpc-spring.png?color=blue)

Using GrpcServerHost
====================
`GrpcServerHost` configures a gRPC Server with services obtained from the `ApplicationContext` and manages that server's 
lifecycle. Services are discovered by finding `BindableService` implementations that are annotated with `@GrpcService`.

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

Using GuavaLFReturnValueHandler
===============================
`GuavaLFReturnValueHandler` teaches Spring Web how to deal with `@Controller` methods that return `ListenableFuture`. 
This allows you to use `ListenableFuture`-based logic end-to-end to build non-blocking asynchronous mvc services on top 
of gRPC.

1. Install `GuavaLFReturnValueHandler` as a `@Bean` in your Spring `@Configuration`.
   ```java
   @Bean
   public GuavaLFReturnValueHandler GuavaLFReturnValueHandler(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
       return new GuavaLFReturnValueHandler().install(requestMappingHandlerAdapter);
   }
   ```
   
2. Return Guava `ListenableFuture`s from your `@Controller` operations.
   ```java
   @Controller
   public class MyController {
       @RequestMapping(method = RequestMethod.GET, value = "/home")
       ListenableFuture<ModelAndView> home(HttpServletRequest request, Model model) {
           // work that returns a ListenableFuture...
       }
   }
   ```
