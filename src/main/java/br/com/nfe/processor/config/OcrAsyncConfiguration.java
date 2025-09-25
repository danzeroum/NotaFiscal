package br.com.nfe.processor.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class OcrAsyncConfiguration {

    @Bean(name = "ocrExecutor")
    public Executor ocrExecutor(
            @Value("${ocr.executor.core-pool-size:1}") int coreSize,
            @Value("${ocr.executor.max-pool-size:2}") int maxSize,
            @Value("${ocr.executor.queue-capacity:10}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ocr-worker-");
        executor.setCorePoolSize(Math.max(1, coreSize));
        executor.setMaxPoolSize(Math.max(executor.getCorePoolSize(), maxSize));
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.afterPropertiesSet();
        return executor;
    }
}
