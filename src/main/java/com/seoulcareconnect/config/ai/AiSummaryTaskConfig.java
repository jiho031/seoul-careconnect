package com.seoulcareconnect.config.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AiSummaryTaskConfig {

    @Bean(name = "aiSummaryTaskExecutor")
    public ThreadPoolTaskExecutor aiSummaryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1_000);
        executor.setThreadNamePrefix("ai-summary-");
        return executor;
    }

    @Bean(name = "aiPolicySummaryRequestExecutor")
    public ThreadPoolTaskExecutor aiPolicySummaryRequestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-summary-request-");
        return executor;
    }
}
