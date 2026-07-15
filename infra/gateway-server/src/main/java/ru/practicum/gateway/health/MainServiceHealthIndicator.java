package ru.practicum.gateway.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MainServiceHealthIndicator implements HealthIndicator {

    private static final String MAIN_SERVICE_ID = "main-service";

    private final DiscoveryClient discoveryClient;

    public MainServiceHealthIndicator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Health health() {
        try {
            List<ServiceInstance> instances =
                    discoveryClient.getInstances(MAIN_SERVICE_ID);

            if (instances.isEmpty()) {
                return Health.down()
                        .withDetail("service", MAIN_SERVICE_ID)
                        .withDetail("instances", 0)
                        .build();
            }

            return Health.up()
                    .withDetail("service", MAIN_SERVICE_ID)
                    .withDetail("instances", instances.size())
                    .build();
        } catch (Exception exception) {
            return Health.down(exception)
                    .withDetail("service", MAIN_SERVICE_ID)
                    .build();
        }
    }
}