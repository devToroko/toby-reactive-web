//package remoteserver;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@SpringBootApplication
//public class RemoteService {
//
//    @Bean
//    TomcatReactiveWebServerFactory tomcatReactiveWebServerFactory() {
//        return new TomcatReactiveWebServerFactory();
//    }
//
//    @RestController
//    public static class RemoteController {
//        @GetMapping("/service")
//        public String service(String req) throws InterruptedException {
//            Thread.sleep(1000);
////            throw new RuntimeException("");
//            return req + "/service1"; // 12. service => service1
//        }
//
//        // 12. 메소드 추가
//        @GetMapping("/service2")
//        public String service2(String req) throws InterruptedException {
//            Thread.sleep(1000);
//            return req + "/service2";
//        }
//    }
//
//    public static void main(String[] args) {
//        // application.properties 값을 따로 쓰고 싶다면 아래처럼 System.setProperty 를 사용하면 된다.
//        System.setProperty("server.port", "8081");
//        System.setProperty("server.tomcat.threads.max", "1000");
////        System.setProperty("spring.devtools.livereload.enabled", "false");
//        SpringApplication.run(RemoteService.class, args);
//    }
//}