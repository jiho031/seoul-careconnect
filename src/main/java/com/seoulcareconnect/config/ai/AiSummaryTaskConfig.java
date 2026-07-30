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
}
