package me.dailycode.tobyreactive;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

@SpringBootApplication
@RestController
@Slf4j
public class TobyReactiveWebApplication {

    @GetMapping("/event/{id}")
//    Mono<Event> hello(@PathVariable long id) {
    Mono<List<Event>> hello(@PathVariable long id) {
        return Mono.just(List.of(new Event(1L, "event1"), new Event(1L, "event1")));
//        return Mono.just(new Event(id, "event " + id));
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> events() {
//        return Flux.fromIterable(List.of(new Event(1L, "event1"), new Event(2L, "event2")));
//        return Flux.just(new Event(1L, "event1"), new Event(1L, "event1"));

//        return Flux
//                .fromStream(Stream.generate(() -> new Event(System.currentTimeMillis(), "value")))
//                .delayElements(Duration.ofSeconds(1))
//                .take(10);

//        Flux<Event> flux =
//                Flux
//                        .<Event>generate(
//                                sink -> {
//                                    sink.next(new Event(System.currentTimeMillis(), "value"));
//                                })
//                        .delayElements(Duration.ofSeconds(1))
//                        .take(10);

//
//        Flux<Event> flux =
//                Flux
//                        .<Event, Long>generate(() -> 1L, (id, sink) -> {
//                            sink.next(new Event(id, "value " + id));
//                            return id+1L;
//                        }).
//                        delayElements(Duration.ofSeconds(1))
//                        .take(10);


//        Flux<Event> es =
//                Flux
//                        .<Event, Long>generate(() -> 1L, (id, sink) -> {
//                            sink.next(new Event(id, "value " + id));
//                            return id+1L;
//                        });
//        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));
//
//        return Flux.zip(es, interval).map(tu -> tu.getT1()).take(10);


        Flux<String> es = Flux.generate(sink -> sink.next("Value"));
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));

        return Flux.zip(es, interval).map(tu -> new Event(tu.getT2(), tu.getT1())).take(10);

    }

    @Data
    @AllArgsConstructor
    public static class Event {
        long id;
        String value;
    }


    public static void main(String[] args) {

        // ReactorNetty 에 무도 기록되어 있음
        // https://projectreactor.io/docs/netty/snapshot/reference/index.html#_metrics_5 참고!
        System.setProperty("reactor.netty.ioWorkerCount", "1");
        System.setProperty("reactor.netty.connection.provider.max.connections", "1000");
        SpringApplication.run(TobyReactiveWebApplication.class, args);
    }

}
