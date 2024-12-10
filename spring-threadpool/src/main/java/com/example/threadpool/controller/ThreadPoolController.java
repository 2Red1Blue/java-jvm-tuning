package com.example.threadpool.controller;

import com.example.threadpool.examples.BasicThreadPoolExample;
import com.example.threadpool.examples.SpringThreadPoolExample;
import com.example.threadpool.service.AsyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api/thread-pool")
public class ThreadPoolController {

    private final BasicThreadPoolExample basicThreadPoolExample;
    private final SpringThreadPoolExample springThreadPoolExample;
    private final AsyncService asyncService;

    public ThreadPoolController(BasicThreadPoolExample basicThreadPoolExample,
                              SpringThreadPoolExample springThreadPoolExample,
                              AsyncService asyncService) {
        this.basicThreadPoolExample = basicThreadPoolExample;
        this.springThreadPoolExample = springThreadPoolExample;
        this.asyncService = asyncService;
    }

    @GetMapping("/basic")
    public String testBasicThreadPools() {
        basicThreadPoolExample.demonstrateBasicThreadPools();
        return "Basic thread pools demonstration completed, check logs for details";
    }

    @GetMapping("/spring")
    public String testSpringThreadPool() {
        springThreadPoolExample.demonstrateSpringThreadPool();
        return "Spring thread pool demonstration completed, check logs for details";
    }

    @GetMapping("/async")
    public String testAsyncThreadPool() throws ExecutionException, InterruptedException {
        log.info("开始异步任务测试");
        
        // 测试异步方法
        CompletableFuture<String> future1 = asyncService.asyncMethod1();
        CompletableFuture<String> future2 = asyncService.asyncMethod2();
        
        // 等待所有异步任务完成
        CompletableFuture.allOf(future1, future2).join();
        
        String result1 = future1.get();
        String result2 = future2.get();
        
        return "Async tasks completed: " + result1 + ", " + result2;
    }
}
