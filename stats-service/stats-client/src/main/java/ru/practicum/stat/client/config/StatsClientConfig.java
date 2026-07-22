package ru.practicum.stat.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class StatsClientConfig {

    @Bean("statsRestTemplate")
    public RestTemplate statsRestTemplate(
            RestTemplateBuilder builder
    ) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Bean("statsServerRetryTemplate")
    public RetryTemplate statsServerRetryTemplate(
            @Value("${stats-server.retry.backoff-period:1000}")
            long backoffPeriod,
            @Value("${stats-server.retry.max-attempts:3}")
            int maxAttempts
    ) {
        FixedBackOffPolicy backOffPolicy =
                new FixedBackOffPolicy();

        backOffPolicy.setBackOffPeriod(backoffPeriod);

        MaxAttemptsRetryPolicy retryPolicy =
                new MaxAttemptsRetryPolicy(maxAttempts);

        RetryTemplate retryTemplate =
                new RetryTemplate();

        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
