package com.example.threadpool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ThreadPoolApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThreadPoolApplication.class, args);
    }
}
