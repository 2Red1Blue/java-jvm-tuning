package com.example.threadpool.examples;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Slf4j
@Component
public class BasicThreadPoolExample {

    /**
     * 演示四种基本的线程池使用方式
     */
    public void demonstrateBasicThreadPools() {
        // 1. 单线程池：保证任务按照提交顺序执行
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        try {
            singleThreadExecutor.submit(() -> {
                log.info("SingleThreadExecutor: 执行任务");
            });
        } finally {
            singleThreadExecutor.shutdown();
        }

        // 2. 固定线程池：适用于负载较重的服务器，可以限制线程数量
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);
        try {
            for (int i = 0; i < 5; i++) {
                final int taskId = i;
                fixedThreadPool.submit(() -> {
                    log.info("FixedThreadPool-{}: 执行任务", taskId);
                });
            }
        } finally {
            fixedThreadPool.shutdown();
        }

        // 3. 缓存线程池：适用于执行大量短期异步任务
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        try {
            for (int i = 0; i < 5; i++) {
                final int taskId = i;
                cachedThreadPool.submit(() -> {
                    log.info("CachedThreadPool-{}: 执行任务", taskId);
                });
            }
        } finally {
            cachedThreadPool.shutdown();
        }

        // 4. 定时任务线程池：适用于需要定期执行的任务
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(2);
        try {
            // 延迟2秒后执行
            scheduledThreadPool.schedule(() -> {
                log.info("ScheduledThreadPool: 执行延迟任务");
            }, 2, TimeUnit.SECONDS);

            // 固定速率执行，每3秒执行一次
            scheduledThreadPool.scheduleAtFixedRate(() -> {
                log.info("ScheduledThreadPool: 执行固定速率任务");
            }, 0, 3, TimeUnit.SECONDS);
        } finally {
            // 等待5秒后关闭，以便观察定时任务的执行
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduledThreadPool.shutdown();
        }
    }
}
