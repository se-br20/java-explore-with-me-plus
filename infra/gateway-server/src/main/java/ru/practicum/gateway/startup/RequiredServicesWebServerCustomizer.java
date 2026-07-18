package ru.practicum.gateway.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class RequiredServicesWebServerCustomizer
        implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory>,
        Ordered {

    private static final Logger log = LoggerFactory.getLogger(
            RequiredServicesWebServerCustomizer.class
    );

    private static final String ENABLED_PROPERTY =
            "gateway.wait-for-required-services";

    private static final String TIMEOUT_PROPERTY =
            "gateway.required-services-timeout-seconds";

    private static final int DEFAULT_TIMEOUT_SECONDS = 90;
    private static final long CHECK_INTERVAL_MILLIS = 500L;

    private static final List<String> REQUIRED_SERVICES = List.of(
            "user-service",
            "event-service",
            "request-service",
            "comment-service"
    );

    private final ObjectProvider<DiscoveryClient> discoveryClientProvider;
    private final Environment environment;

    public RequiredServicesWebServerCustomizer(
            ObjectProvider<DiscoveryClient> discoveryClientProvider,
            Environment environment
    ) {
        this.discoveryClientProvider = discoveryClientProvider;
        this.environment = environment;
    }

    @Override
    public void customize(NettyReactiveWebServerFactory factory) {
        boolean enabled = environment.getProperty(
                ENABLED_PROPERTY,
                Boolean.class,
                false
        );

        if (!enabled) {
            return;
        }

        factory.addServerCustomizers(httpServer -> {
            waitForRequiredServices();
            return httpServer;
        });
    }

    private void waitForRequiredServices() {
        DiscoveryClient discoveryClient =
                discoveryClientProvider.getIfAvailable();

        if (discoveryClient == null) {
            throw new IllegalStateException(
                    "DiscoveryClient is not available in gateway-server"
            );
        }

        int timeoutSeconds = environment.getProperty(
                TIMEOUT_PROPERTY,
                Integer.class,
                DEFAULT_TIMEOUT_SECONDS
        );

        long deadline = System.nanoTime()
                + Duration.ofSeconds(timeoutSeconds).toNanos();

        while (System.nanoTime() < deadline) {
            List<String> missingServices = REQUIRED_SERVICES.stream()
                    .filter(serviceId -> discoveryClient
                            .getInstances(serviceId)
                            .isEmpty())
                    .toList();

            if (missingServices.isEmpty()) {
                log.info(
                        "Gateway DiscoveryClient contains all services: {}",
                        REQUIRED_SERVICES
                );
                return;
            }

            log.info(
                    "Gateway DiscoveryClient is waiting for services: {}",
                    missingServices
            );

            sleepBeforeNextCheck();
        }

        throw new IllegalStateException(
                "Gateway DiscoveryClient did not receive required services "
                        + REQUIRED_SERVICES
                        + " within "
                        + timeoutSeconds
                        + " seconds"
        );
    }

    private void sleepBeforeNextCheck() {
        try {
            Thread.sleep(CHECK_INTERVAL_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Waiting for required services was interrupted",
                    exception
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}