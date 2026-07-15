package ru.practicum.stat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
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

    private final RestTemplate template;
    private final String statsUrl;
    private final String appName;

    public StatsClient(
            RestTemplate template,
            @Value("${stats-server.url}") String statsUrl,
            @Value("${app.name}") String appName
    ) {
        this.template = template;
        this.statsUrl = statsUrl;
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
            HttpEntity<EndpointHitDto> requestEntity =
                    new HttpEntity<>(requestBody);

            template.exchange(
                    statsUrl + "/hit",
                    POST,
                    requestEntity,
                    Void.class
            );

            log.debug(
                    "Hit saved: app={}, uri={}, ip={}, timestamp={}",
                    requestBody.getApp(),
                    requestBody.getUri(),
                    requestBody.getIp(),
                    requestBody.getTimestamp()
            );
        } catch (RestClientException exception) {
            log.error(
                    "Failed to save hit to {}: {}",
                    statsUrl,
                    exception.getMessage(),
                    exception
            );
        }
    }

    public List<ViewStatsDto> get(ParamDto paramDto) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(statsUrl)
                .path("/stats")
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

        URI uri = builder
                .build()
                .encode()
                .toUri();

        try {
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
        } catch (RestClientException exception) {
            log.error(
                    "Failed to receive stats from {}: {}",
                    uri,
                    exception.getMessage(),
                    exception
            );

            return List.of();
        }
    }
}