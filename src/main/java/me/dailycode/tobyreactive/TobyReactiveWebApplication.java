package me.dailycode.tobyreactive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

@SpringBootApplication
@RestController
@Slf4j
@EnableAsync
public class TobyReactiveWebApplication {

    public static final String URL1 = "http://localhost:8081/service?req={req}";
    public static final String URL2 = "http://localhost:8081/service2?req={req}";

    @GetMapping("/")
    Mono<String> hello() {
        log.info("pos1");
//        Mono<String> m = Mono.just("Hello WebFlux").log();
        // just 는 이미 다 준비된 Publishing Data 를 인자로 받기 때문에
        // 아래 처럼하면 myService.findById(1) 가 먼저 실행되고 그 결과값이 just 인자로 들어가는 것이다.
//        Mono<String> m = Mono.just(myService.findById(1)).log();
        // 어렵다면 아래처럼 생각하자.
//        String s = generateHello();
//        Mono<String> m = Mono.just(s).log();


        // 콜백스타일을 쓰면 myService.findById(1) 도 subscribe 시점에 동작하도록 할 수 있다.
        Mono<String> m =
                Mono.fromSupplier(this::generateHello)//subscribe 하는 시점에 실제 수행
                        .doOnNext(log::info)
                        .log();
        // cold publisher 이므로 여기서 한번 수행하고,
        // return 에 의한 spring의 subscribe 에 의해 한번더 수행된다.
        // 이런 cold publisher 는 데이터가 다 준비되고, 그 "준비된 데이터"를
        // subscribe 가 일어나는 시점에 처음부터 끝까지 다 건네주는 게 특징이다.
        m.subscribe();

//        m.block(); // 이제 이 기능은 안된다!!! 완전히 막힘!

        // 그렇다면 Hot Source 는? 실시간 이벤트! 가 대표적이다.
        // 준비된 데이터가 아니라 정말 계속해서 생성될 수도 있는 데이터다.


        log.info("pos2");
        return m;
    }

    private String generateHello() {
        log.info("method generateHello()");
        return "Hello Mono";
    }

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
        return client.get().uri(URL1, idx).exchange()
                .doOnNext(res -> log.info(Thread.currentThread().getName() + " doOnNext")) // 디버깅도 쉽다!
                .flatMap(c -> c.bodyToMono(String.class))
                .flatMap(res1 -> client.get().uri(URL2, res1).exchange())
                .flatMap(c -> c.bodyToMono(String.class))
                .doOnNext(log::info) // 쓰레드: [reactor-http-nio-1]
                .flatMap(res2 -> Mono.fromCompletionStage(myService.work(res2)))
                .doOnNext(log::info); // 쓰레드: [task-*] myService.work(res2)) 를 실제 수행하는 쓰레드가 다르다는 것을 알 수 있다.
                // 하나의 netty worker 쓰레드를 쓰는데, 이게 블로킹되면 안된다.
                // 그래서 위에서 어디서도 블로킹이 일어나지 않도록 한다!

                // API 호출하는 동안 하나의 쓰레드가 묶여있지 않다는 것. 그게 가장 중요하다!
    }

    @Service
    public static class MyService {
        @Async // Future, ListenableFuture, CompletableFuture 셋 중 하나로 반환 ==> 쓰려면 @EnableAsync 를 써야한다!
        public CompletableFuture<String> work(String req) {
            return CompletableFuture.completedFuture(req + "asyncWork");
//            return req + "/asyncWork";
        }
    }

    @Autowired MyService myService;

    public static void main(String[] args) {
//        ReactorNetty
//        org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration
        // WebFluxAutoConfiguration 를 참조하자.
        // ReactorNetty 에 무도 기록되어 있음
        // https://projectreactor.io/docs/netty/snapshot/reference/index.html#_metrics_5 참고!
        System.setProperty("reactor.netty.ioWorkerCount", "1");
        System.setProperty("reactor.netty.connection.provider.max.connections", "1000");
        SpringApplication.run(TobyReactiveWebApplication.class, args);
    }

}
