package com.example.threadpool.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CompleteAsyncService {

    private final ThreadPoolExecutor customThreadPoolExecutor;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public CompleteAsyncService(ThreadPoolExecutor customThreadPoolExecutor,
                              ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.customThreadPoolExecutor = customThreadPoolExecutor;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    @Async("threadPoolTaskExecutor")
    @Retryable(value = {Exception.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000))
    public CompletableFuture<String> asyncMethodWithRetry() {
        log.info("执行异步任务（带重试机制）");
        // 模拟可能失败的操作
        if (Math.random() < 0.5) {
            throw new RuntimeException("模拟任务失败");
        }
        return CompletableFuture.completedFuture("任务成功");
    }

    @Recover
    public CompletableFuture<String> recover(Exception e) {
        log.error("重试后仍然失败，执行恢复操作", e);
        return CompletableFuture.completedFuture("恢复后的结果");
    }

    // 分表查询示例 - 最佳实践
    public List<String> parallelQueryAndSort(List<String> tableNames) {
        try {
            // 使用CompletableFuture进行并行查询
            List<CompletableFuture<List<String>>> futures = tableNames.stream()
                .map(tableName -> CompletableFuture.supplyAsync(() -> 
                    queryTable(tableName), threadPoolTaskExecutor))
                .collect(Collectors.toList());

            // 等待所有查询完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

            // 获取并合并结果
            return allOf.thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .sorted()
                .collect(Collectors.toList())
            ).get(30, TimeUnit.SECONDS); // 设置超时时间

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询被中断", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("查询执行失败", e);
        }
    }

    private List<String> queryTable(String tableName) {
        // 模拟数据库查询
        try {
            Thread.sleep(1000);
            return List.of("Data from " + tableName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询中断", e);
        }
    }

    // 展示如何正确处理CompletableFuture的异常
    public void handleAsyncException() {
        CompletableFuture.supplyAsync(() -> {
            // 可能抛出异常的业务逻辑
            throw new RuntimeException("Async operation failed");
        }, threadPoolTaskExecutor)
        .exceptionally(throwable -> {
            log.error("异步操作异常", throwable);
            return "默认值";
        })
        .thenAccept(result -> log.info("处理结果: {}", result));
    }
}
