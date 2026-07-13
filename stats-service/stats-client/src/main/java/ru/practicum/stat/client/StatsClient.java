package ru.practicum.stat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.dto.ParamDto;
import ru.practicum.stat.dto.ViewStatsDto;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Slf4j
@Component
public class StatsClient {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;
    private final String appName;

    public StatsClient(
            RestTemplate restTemplate,
            DiscoveryClient discoveryClient,
            @Value("${stats-service.id:stats-server}")
            String statsServiceId,
            @Value("${app.name:ewm-main-service}")
            String appName
    ) {
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.appName = appName;
        this.retryTemplate = createRetryTemplate();
    }

    public void hit(EndpointHitDto endpointHit) {
        endpointHit.setApp(appName);

        try {
            URI uri = makeUri("/hit");

            HttpEntity<EndpointHitDto> request =
                    new HttpEntity<>(endpointHit);

            restTemplate.exchange(
                    uri,
                    POST,
                    request,
                    Void.class
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Не удалось сохранить обращение "
                            + "в stats-server",
                    exception
            );
        }
    }

    public List<ViewStatsDto> get(ParamDto paramDto) {
        if (paramDto == null
                || paramDto.getStart() == null
                || paramDto.getEnd() == null) {
            log.warn(
                    "Невозможно запросить статистику: "
                            + "не заданы start или end"
            );

            return List.of();
        }

        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder
                            .fromUri(makeUri("/stats"))
                            .queryParam(
                                    "start",
                                    paramDto.getStart()
                                            .format(FORMATTER)
                            )
                            .queryParam(
                                    "end",
                                    paramDto.getEnd()
                                            .format(FORMATTER)
                            )
                            .queryParam(
                                    "unique",
                                    Boolean.TRUE.equals(
                                            paramDto.getUnique()
                                    )
                            );

            if (paramDto.getUris() != null
                    && paramDto.getUris().length > 0) {
                builder.queryParam(
                        "uris",
                        (Object[]) paramDto.getUris()
                );
            }

            URI uri = builder
                    .build()
                    .encode()
                    .toUri();

            ResponseEntity<List<ViewStatsDto>> response =
                    restTemplate.exchange(
                            uri,
                            GET,
                            null,
                            new ParameterizedTypeReference<>() {
                            }
                    );

            if (response.getBody() == null) {
                return List.of();
            }

            return response.getBody();
        } catch (RuntimeException exception) {
            log.warn(
                    "Не удалось получить статистику "
                            + "из stats-server",
                    exception
            );

            return List.of();
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance =
                retryTemplate.execute(
                        context -> getInstance()
                );

        return UriComponentsBuilder
                .fromUri(instance.getUri())
                .path(path)
                .build()
                .toUri();
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances =
                discoveryClient.getInstances(
                        statsServiceId
                );

        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException(
                    "В Eureka не найден сервис: "
                            + statsServiceId
            );
        }

        return instances.getFirst();
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate template =
                new RetryTemplate();

        FixedBackOffPolicy backOffPolicy =
                new FixedBackOffPolicy();

        backOffPolicy.setBackOffPeriod(3000L);

        template.setBackOffPolicy(
                backOffPolicy
        );

        MaxAttemptsRetryPolicy retryPolicy =
                new MaxAttemptsRetryPolicy();

        retryPolicy.setMaxAttempts(3);

        template.setRetryPolicy(
                retryPolicy
        );

        return template;
    }
}