package com.example.qbot.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class ThreadPoolUtil {

    @Bean("scheduledThreadPool")
    public ScheduledExecutorService createScheduledThreadPool() {
        return new ScheduledThreadPoolExecutor(3);
    }

}
