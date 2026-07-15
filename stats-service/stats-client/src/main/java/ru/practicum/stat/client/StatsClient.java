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
import org.springframework.web.client.RestClientException;
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

@Component
@Slf4j
public class StatsClient {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_PERIOD = 3000L;

    private final RestTemplate template;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;
    private final String appName;

    public StatsClient(
            RestTemplate template,
            DiscoveryClient discoveryClient,
            @Value("${stats-server.service-id:stats-server}")
            String statsServiceId,
            @Value("${app.name}")
            String appName
    ) {
        this.template = template;
        this.discoveryClient = discoveryClient;
        this.retryTemplate = createRetryTemplate();
        this.statsServiceId = statsServiceId;
        this.appName = appName;
    }

    public void hit(EndpointHitDto endpointHit) {
        EndpointHitDto requestBody = EndpointHitDto.builder()
                .app(appName)
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(endpointHit.getTimestamp())
                .build();

        try {
            URI targetUri = makeUri("/hit");

            HttpEntity<EndpointHitDto> requestEntity =
                    new HttpEntity<>(requestBody);

            template.exchange(
                    targetUri,
                    POST,
                    requestEntity,
                    Void.class
            );

            log.debug(
                    "Hit saved to {}: app={}, uri={}, ip={}, timestamp={}",
                    targetUri,
                    requestBody.getApp(),
                    requestBody.getUri(),
                    requestBody.getIp(),
                    requestBody.getTimestamp()
            );
        } catch (RestClientException
                 | StatsServerUnavailableException exception) {

            log.warn(
                    "Failed to save hit through service {}: {}",
                    statsServiceId,
                    exception.getMessage()
            );
        }
    }

    public List<ViewStatsDto> get(ParamDto paramDto) {
        try {
            URI uri = buildStatsUri(paramDto);

            ResponseEntity<List<ViewStatsDto>> response =
                    template.exchange(
                            uri,
                            GET,
                            null,
                            new ParameterizedTypeReference<>() {
                            }
                    );

            List<ViewStatsDto> body = response.getBody();

            log.debug(
                    "Stats received: request={}, status={}, body={}",
                    uri,
                    response.getStatusCode(),
                    body
            );

            return body != null ? body : List.of();
        } catch (RestClientException
                 | StatsServerUnavailableException exception) {

            log.warn(
                    "Failed to receive stats through service {}: {}",
                    statsServiceId,
                    exception.getMessage()
            );

            return List.of();
        }
    }

    private URI buildStatsUri(ParamDto paramDto) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUri(makeUri("/stats"))
                .queryParam(
                        "start",
                        paramDto.getStart().format(FORMATTER)
                )
                .queryParam(
                        "end",
                        paramDto.getEnd().format(FORMATTER)
                )
                .queryParam(
                        "unique",
                        Boolean.TRUE.equals(paramDto.getUnique())
                );

        if (paramDto.getUris() != null) {
            for (String uri : paramDto.getUris()) {
                builder.queryParam("uris", uri);
            }
        }

        return builder
                .build()
                .encode()
                .toUri();
    }

    private URI makeUri(String path) {

        ServiceInstance instance = retryTemplate.execute(
                context -> getInstance()
        );

        return URI.create(
                "http://"
                        + instance.getHost()
                        + ":"
                        + instance.getPort()
                        + path
        );
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances =
                    discoveryClient.getInstances(statsServiceId);

            if (instances.isEmpty()) {
                throw new StatsServerUnavailableException(
                        "No instances found for service: "
                                + statsServiceId
                );
            }

            return instances.get(0);
        } catch (StatsServerUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new StatsServerUnavailableException(
                    "Failed to discover service: "
                            + statsServiceId,
                    exception
            );
        }
    }

    private static RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy =
                new FixedBackOffPolicy();

        backOffPolicy.setBackOffPeriod(BACKOFF_PERIOD);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy =
                new MaxAttemptsRetryPolicy();

        retryPolicy.setMaxAttempts(MAX_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    private static final class StatsServerUnavailableException
            extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private StatsServerUnavailableException(String message) {
            super(message);
        }

        private StatsServerUnavailableException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }
}