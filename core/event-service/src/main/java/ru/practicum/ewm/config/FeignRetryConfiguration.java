package ru.practicum.ewm.config;

import feign.Response;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FeignRetryConfiguration {

    private static final long INITIAL_INTERVAL_MILLIS = 200L;
    private static final long MAX_INTERVAL_MILLIS = 1000L;
    private static final int MAX_ATTEMPTS = 3;

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(
                INITIAL_INTERVAL_MILLIS,
                MAX_INTERVAL_MILLIS,
                MAX_ATTEMPTS
        );
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        ErrorDecoder defaultDecoder =
                new ErrorDecoder.Default();

        return (methodKey, response) -> {
            if (isServerError(response)) {
                return new RetryableException(
                        response.status(),
                        "Retryable HTTP "
                                + response.status()
                                + " during "
                                + methodKey,
                        response.request().httpMethod(),
                        null,
                        (Long) null,
                        response.request()
                );
            }

            return defaultDecoder.decode(
                    methodKey,
                    response
            );
        };
    }

    private boolean isServerError(Response response) {
        return response.status() >= 500
                && response.status() <= 599;
    }
}