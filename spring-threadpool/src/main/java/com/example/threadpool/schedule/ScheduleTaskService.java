package com.example.threadpool.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ScheduleTaskService {

    private final TaskScheduler taskScheduler;

    public ScheduleTaskService(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    // 使用@Scheduled注解的固定速率执行
    @Scheduled(fixedRate = 5000) // 每5秒执行一次
    public void fixedRateTask() {
        log.info("Fixed rate task executed at: {}", LocalDateTime.now());
    }

    // 使用@Scheduled注解的Cron表达式
    @Scheduled(cron = "0 0 12 * * ?") // 每天中午12点执行
    public void cronTask() {
        log.info("Cron task executed at: {}", LocalDateTime.now());
    }

    // 使用TaskScheduler的动态调度
    public void scheduleTaskDynamically() {
        // 方式1：使用CronTrigger
        taskScheduler.schedule(
            () -> log.info("Dynamic cron task executed at: {}", LocalDateTime.now()),
            new CronTrigger("0/10 * * * * ?") // 每10秒执行一次
        );

        // 方式2：使用PeriodicTrigger
        taskScheduler.schedule(
            () -> log.info("Dynamic periodic task executed at: {}", LocalDateTime.now()),
            new PeriodicTrigger(15, TimeUnit.SECONDS) // 每15秒执行一次
        );
    }

    // 心跳检测示例
    public void startHeartbeatCheck() {
        taskScheduler.scheduleWithFixedDelay(
            () -> {
                try {
                    performHeartbeatCheck();
                } catch (Exception e) {
                    log.error("心跳检测失败", e);
                }
            },
            TimeUnit.SECONDS.toMillis(30) // 每30秒执行一次
        );
    }

    private void performHeartbeatCheck() {
        log.info("执行心跳检测: {}", LocalDateTime.now());
        // 实际的心跳检测逻辑
    }
}
