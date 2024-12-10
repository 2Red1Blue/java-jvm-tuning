package com.example.threadpool.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Value("${thread-pool.core-pool-size}")
    private int corePoolSize;

    @Value("${thread-pool.max-pool-size}")
    private int maxPoolSize;

    @Value("${thread-pool.queue-capacity}")
    private int queueCapacity;

    @Value("${thread-pool.keep-alive-seconds}")
    private int keepAliveSeconds;

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(corePoolSize);
        // 最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        // 队列容量
        executor.setQueueCapacity(queueCapacity);
        // 线程空闲时间
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 线程名前缀
        executor.setThreadNamePrefix("spring-thread-");
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
