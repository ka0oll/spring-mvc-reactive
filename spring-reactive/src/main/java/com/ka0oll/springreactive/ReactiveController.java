package com.ka0oll.springreactive;


import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Slf4j
@RestController
@RequestMapping()
public class ReactiveController {

    WebClient webClient = WebClient.create("http://localhost:8081");

    @GetMapping("/mono")
    public Mono<String> webflux(){
        //스프링 네티 이벤트 thread
        log.info("webflux() method call thread : {}", Thread.currentThread().getName());

        //실제 결과는 스프링이 구독 네티 이벤트 thread,
        return webClient.get().uri("/sleep").exchange().log()
            .flatMap(clientResponse -> {
                //webclient의 쓰레드
                log.info("flatMap() method call thread : {}", Thread.currentThread().getName());
                return clientResponse.bodyToMono(String.class);
            })
            .flatMap(s-> webClient.get().uri("/sleep").exchange())
            .flatMap(clientResponse -> {
                //webclient의 쓰레드
                log.info("flatMap() method call thread : {}", Thread.currentThread().getName());
                return clientResponse.bodyToMono(String.class);
            });
    }

    @GetMapping("/flux")
    public Flux<String> webflux2(){
        log.info("webflux() method call thread : {}", Thread.currentThread().getName());

        Mono<String> first = webClient.get().uri("/sleep").exchange()
            .flatMap(clientResponse -> {
                log.info("flatMap() method call thread : {}", Thread.currentThread().getName());
                return clientResponse.bodyToMono(String.class);
            });

        Mono<String> second = webClient.get().uri("/sleep").exchange()
            .flatMap(clientResponse -> {
                log.info("flatMap() method call thread : {}", Thread.currentThread().getName());
                return clientResponse.bodyToMono(String.class);
            });

        Mono<String> third = webClient.get().uri("/sleep").exchange()
            .flatMap(clientResponse -> {
                log.info("flatMap() method call thread : {}", Thread.currentThread().getName());
                return clientResponse.bodyToMono(String.class);
            });

        return Flux.merge(first, second,third).log();
    }

    public static void main(String[] args) throws BrokenBarrierException, InterruptedException {

        int count = 50;
        String url = "http://localhost:8080/flux";
        CyclicBarrier barrier = new CyclicBarrier(count+1);
        ExecutorService executor = Executors.newFixedThreadPool(count);
        IntStream.range(0, count).forEach(value -> executor.submit(() -> {
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }

            long startTime = System.currentTimeMillis();
            RestTemplate restTemplate = new RestTemplate();
            String body = restTemplate.getForEntity(url, String.class).getBody();

            long diff = System.currentTimeMillis() - startTime;
            System.out.println("latency : " +diff);
        }));

        barrier.await();

        executor.shutdown();
    }

}
