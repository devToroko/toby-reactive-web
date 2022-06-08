package me.dailycode.tobyreactive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.stream.Stream;

@SpringBootApplication
@RestController
@Slf4j
public class TobyReactiveWebApplication {

    public static final String URL1 = "http://localhost:8081/service?req={req}";
    public static final String URL2 = "http://localhost:8081/service2?req={req}";

    @Bean
    NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        return new NettyReactiveWebServerFactory();
    }

    WebClient client = WebClient.create();

    @GetMapping("/rest")
    public Mono<String> restEx(@RequestParam(required = false) int idx) {
        // 1. subscribe 하지 않는 이상, 즉 리턴해서 프레임워크가 subscribe 하지 않는 이상
        // 실제 rest api 호출이 일어나지 않는다!
        // RestTemplate 와 Mono 사용의 가장 큰 차이점은 이거라고 본다.
        // Mono<ClientResponse> res = client.get().uri(URL1, idx).exchange();

        // 2. 이러면 위의 Mono 는 무시. 실행도 안됨!
        // return Mono.just("Hello");

        // 3. Spring 이 Mono 타입을 보면, subscribe 를 호출한다.

        // 4. Mono 안에 담겨진 ClientResponse 가 내포하고 있는 String 값을 Mono<String> 으로 다시 감싸서 반환하고 싶다면...
        // flatMap 도 결국 Mono 로 반환되고, 이것도 subscribe 전까지는 flatMap 은 일어나지 않는다.
        //Mono<String> body = res.flatMap(clientResponse -> {
        //    Mono<String> stringMono = clientResponse.bodyToMono(String.class);
        //    return stringMono;
        //});
        //return body;

        // 한줄로 하면 아래와 같다.
//        return client.get()
//                .uri(URL1, idx)
//                .exchangeToMono(response -> {
//                    return response.bodyToMono(String.class);
//                });

        // 강의에서는 아래처럼
//        return client.get().uri(URL1, idx).exchange()
//                .flatMap(c -> c.bodyToMono(String.class));

        // 그런데 의존적인 API 호출은 어떻게 설정할까?
        log.info(Thread.currentThread().getName() + " from outside " + idx);
        return client.get().uri(URL1, idx).exchange()
                .flatMap(c -> c.bodyToMono(String.class))
                .flatMap(res1 -> {
                    log.info(Thread.currentThread().getName() + " from inside " + idx);
                    return client.get().uri(URL2, res1).exchange();
                })
                .flatMap(c -> c.bodyToMono(String.class));

    }

    @Service
    public static class MyService {
        public String work(String req) {
            return req + "/asyncWork";
        }
    }

    @Autowired MyService myService;

    public static void main(String[] args) {
        System.setProperty("reactor.netty.ioWorkerCount", "1");
        System.setProperty("reactor.ipc.netty.pool.maxConnections", "2000");
        SpringApplication.run(TobyReactiveWebApplication.class, args);
    }

}
