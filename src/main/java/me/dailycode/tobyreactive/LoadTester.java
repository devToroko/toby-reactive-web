package me.dailycode.tobyreactive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LoadTester {
    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {

        ExecutorService es = Executors.newFixedThreadPool(100, new CustomizableThreadFactory("loadTester-"));

        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:8080/rest?idx={idx}";

        CyclicBarrier barrier = new CyclicBarrier(101);

        StopWatch mainWatch = new StopWatch();
        mainWatch.start();

        for (int i = 0; i < 100; i++) {
            es.submit(() -> {
                int idx = counter.addAndGet(1);
                barrier.await();

                log.info("Thread {}", idx);
                StopWatch threadWatch = new StopWatch();
                threadWatch.start();

                String res = rt.getForObject(url, String.class, idx);

                threadWatch.stop();
                log.info("Thread {} -> Elapsed: {} / {}", idx, threadWatch.getTotalTimeSeconds(), res);

                return (Void)null;
            });
        }

        barrier.await();
        es.shutdown();
        es.awaitTermination(100, TimeUnit.SECONDS);

        mainWatch.stop();
        log.info("Total: {}", mainWatch.getTotalTimeSeconds());

    }
}
