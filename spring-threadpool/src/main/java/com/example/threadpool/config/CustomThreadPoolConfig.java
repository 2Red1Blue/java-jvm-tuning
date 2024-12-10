package com.example.threadpool.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class CustomThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor customThreadPoolExecutor() {
        return new ThreadPoolExecutor(
            5,                      // 核心线程数
            10,                     // 最大线程数
            60L,                    // 空闲线程存活时间
            TimeUnit.SECONDS,       // 时间单位
            new LinkedBlockingQueue<>(25), // 任务队列
            new ThreadFactory() {   // 自定义线程工厂
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("custom-thread-" + count++);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
}
