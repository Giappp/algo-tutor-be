package org.rap.algotutorbe.judge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class JudgeExecutorConfig {

    /**
     * Thread pool for parallel testcase execution.
     * Core pool: 4 threads (handles typical workload)
     * Max pool: 8 threads (burst capacity)
     * Queue: 50 tasks (backpressure before rejection)
     */
    @Bean(name = "judgeExecutor")
    public Executor judgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("judge-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
