package ru.practicum.stat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stat.client.exception.StatsServerUnavailableException;
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

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;
    private final String appName;

    public StatsClient(
            @Qualifier("statsRestTemplate")
            RestTemplate restTemplate,
            DiscoveryClient discoveryClient,
            @Qualifier("statsServerRetryTemplate")
            RetryTemplate retryTemplate,
            @Value("${stats-server.service-id:stats-server}")
            String statsServiceId,
            @Value("${spring.application.name:event-service}")
            String appName
    ) {
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
        this.retryTemplate = retryTemplate;
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
            URI uri = makeUri("/hit");

            restTemplate.exchange(
                    uri,
                    POST,
                    new HttpEntity<>(requestBody),
                    Void.class
            );
        } catch (RestClientException
                 | StatsServerUnavailableException exception) {

            log.warn(
                    "Legacy stats hit was not sent: serviceId={}, reason={}",
                    statsServiceId,
                    exception.getMessage()
            );
        }
    }

    public List<ViewStatsDto> get(ParamDto paramDto) {
        try {
            URI uri = buildStatsUri(paramDto);

            ResponseEntity<List<ViewStatsDto>> response =
                    restTemplate.exchange(
                            uri,
                            GET,
                            null,
                            new ParameterizedTypeReference<>() {
                            }
                    );

            List<ViewStatsDto> body = response.getBody();
            return body == null ? List.of() : body;
        } catch (RestClientException
                 | StatsServerUnavailableException exception) {

            log.warn(
                    "Legacy stats were not received: serviceId={}, reason={}",
                    statsServiceId,
                    exception.getMessage()
            );

            return List.of();
        }
    }

    private URI buildStatsUri(ParamDto paramDto) {
        UriComponentsBuilder builder =
                UriComponentsBuilder
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

        return builder.build().encode().toUri();
    }

    private URI makeUri(String path) {
        ServiceInstance instance =
                retryTemplate.execute(context -> getInstance());

        return UriComponentsBuilder
                .fromUri(instance.getUri())
                .path(path)
                .build()
                .toUri();
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances;

        try {
            instances =
                    discoveryClient.getInstances(statsServiceId);
        } catch (RuntimeException exception) {
            throw new StatsServerUnavailableException(
                    "Failed to discover stats-server",
                    exception
            );
        }

        if (instances.isEmpty()) {
            throw new StatsServerUnavailableException(
                    "No stats-server instances found"
            );
        }

        return instances.getFirst();
    }
}
