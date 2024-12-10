package com.example.threadpool.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AsyncService {

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<String> asyncMethod1() {
        log.info("执行异步任务1");
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return CompletableFuture.completedFuture("异步任务1完成");
    }

    @Async("customThreadPoolExecutor")
    public CompletableFuture<String> asyncMethod2() {
        log.info("执行异步任务2");
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return CompletableFuture.completedFuture("异步任务2完成");
    }
}
