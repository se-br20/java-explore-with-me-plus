package ru.practicum.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RequiredServicesReadinessFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(
            RequiredServicesReadinessFilter.class
    );

    private static final String ENABLED_PROPERTY =
            "gateway.wait-for-required-services";

    private static final String TIMEOUT_PROPERTY =
            "gateway.required-services-timeout-seconds";

    private static final int DEFAULT_TIMEOUT_SECONDS = 45;

    private static final Duration CHECK_INTERVAL =
            Duration.ofMillis(250);

    private static final List<String> REQUIRED_SERVICES = List.of(
            "user-service",
            "event-service",
            "request-service",
            "comment-service"
    );

    private final DiscoveryClient discoveryClient;
    private final Environment environment;
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public RequiredServicesReadinessFilter(
            DiscoveryClient discoveryClient,
            Environment environment
    ) {
        this.discoveryClient = discoveryClient;
        this.environment = environment;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        if (!isEnabled() || ready.get()) {
            return chain.filter(exchange);
        }

        return waitForRequiredServices()
                .then(chain.filter(exchange));
    }

    private Mono<Void> waitForRequiredServices() {
        int timeoutSeconds = resolveTimeoutSeconds();

        return Flux.interval(Duration.ZERO, CHECK_INTERVAL)
                .concatMap(ignored -> findMissingServices())
                .distinctUntilChanged()
                .doOnNext(this::logCurrentState)
                .filter(List::isEmpty)
                .next()
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnNext(ignored -> markGatewayReady())
                .onErrorMap(
                        TimeoutException.class,
                        exception -> createUnavailableException(
                                timeoutSeconds,
                                exception
                        )
                )
                .then();
    }

    private Mono<List<String>> findMissingServices() {
        return Mono.fromCallable(() -> REQUIRED_SERVICES.stream()
                        .filter(serviceId -> discoveryClient
                                .getInstances(serviceId)
                                .isEmpty())
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void logCurrentState(List<String> missingServices) {
        if (missingServices.isEmpty()) {
            return;
        }

        log.info(
                "Gateway local DiscoveryClient is waiting for services: {}",
                missingServices
        );
    }

    private void markGatewayReady() {
        if (ready.compareAndSet(false, true)) {
            log.info(
                    "Gateway local DiscoveryClient contains all services: {}",
                    REQUIRED_SERVICES
            );
        }
    }

    private boolean isEnabled() {
        return environment.getProperty(
                ENABLED_PROPERTY,
                Boolean.class,
                false
        );
    }

    private int resolveTimeoutSeconds() {
        int timeoutSeconds = environment.getProperty(
                TIMEOUT_PROPERTY,
                Integer.class,
                DEFAULT_TIMEOUT_SECONDS
        );

        if (timeoutSeconds < 1) {
            return DEFAULT_TIMEOUT_SECONDS;
        }

        return timeoutSeconds;
    }

    private ResponseStatusException createUnavailableException(
            int timeoutSeconds,
            TimeoutException exception
    ) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Required services were not discovered within "
                        + timeoutSeconds
                        + " seconds",
                exception
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}