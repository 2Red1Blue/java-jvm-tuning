package com.example.threadpool.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncExecutorConfig implements AsyncConfigurer {

    @Value("${thread-pool.core-pool-size}")
    private int corePoolSize;

    @Value("${thread-pool.max-pool-size}")
    private int maxPoolSize;

    @Value("${thread-pool.queue-capacity}")
    private int queueCapacity;

    @Value("${thread-pool.keep-alive-seconds}")
    private int keepAliveSeconds;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            // 处理未捕获的异步异常
            System.err.println("Async method " + method.getName() + " threw exception: " + ex.getMessage());
        };
    }
}
