package com.example.threadpool.examples;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class SpringThreadPoolExample {

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public SpringThreadPoolExample(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    public void demonstrateSpringThreadPool() {
        // 1. 直接提交任务
        threadPoolTaskExecutor.execute(() -> {
            log.info("Spring ThreadPool: 执行简单任务");
        });

        // 2. 提交有返回值的任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            log.info("Spring ThreadPool: 执行异步任务");
            return "任务完成";
        }, threadPoolTaskExecutor);

        future.thenAccept(result -> log.info("任务结果: {}", result));

        // 3. 查看线程池状态
        log.info("当前活跃线程数: {}", threadPoolTaskExecutor.getActiveCount());
        log.info("核心线程数: {}", threadPoolTaskExecutor.getCorePoolSize());
        log.info("最大线程数: {}", threadPoolTaskExecutor.getMaxPoolSize());
        log.info("队列大小: {}", threadPoolTaskExecutor.getQueueSize());
    }
}
