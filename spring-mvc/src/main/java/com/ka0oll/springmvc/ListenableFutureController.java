package com.ka0oll.springmvc;

import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping()
public class ListenableFutureController {

    AsyncRestTemplate nioAsyncRestTemplate = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

    @GetMapping("/none-block")
    public ListenableFuture<ResponseEntity<String>> noneBlock() {
        //톰캣 쓰레드
        log.info("requestThread : {}", Thread.currentThread().getName());
        ListenableFuture<ResponseEntity<String>> listenableFuture = nioAsyncRestTemplate.getForEntity("http://localhost:8081/sleep", String.class);
        listenableFuture.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
             @Override
             public void onFailure(Throwable ex) {

             }

             @Override
             public void onSuccess(ResponseEntity<String> result) {
                 // 네티 쓰레드이다. callback까지는 evnetLoop쓰레드가 하고 결과값 셋팅하고 그후로는 tomcat쓰레드가 진행한다.
                 log.info("onSuccessThread : {}", Thread.currentThread().getName());
             }
         });

        //실제 response는 tomcat Thread가 처리한다.
        return listenableFuture;
    }

    @GetMapping("/block")
    public String block() {
        new RestTemplate().getForEntity("http://localhost:8081/sleep", String.class);
        return "test";
    }


    public static void main(String[] args) throws BrokenBarrierException, InterruptedException {

        int count = 1;
        //String url = "http://localhost:8080/block";
        String url = "http://localhost:8080/none-block";
        CyclicBarrier barrier = new CyclicBarrier(count+1);
        ExecutorService executor = Executors.newFixedThreadPool(100);
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
            restTemplate.getForEntity(url, String.class);

            long diff = System.currentTimeMillis() - startTime;
            System.out.println("latency : " +diff);
        }));

        barrier.await();

        executor.shutdown();
    }

}
