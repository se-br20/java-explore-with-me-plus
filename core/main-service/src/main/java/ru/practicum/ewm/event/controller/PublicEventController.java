package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.paramDto.PublicUserEventParam;
import ru.practicum.ewm.event.service.EventService;

import java.util.List;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/events")
public class PublicEventController {

    private static final String X_CLIENT_IP = "X-Client-IP";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(
            @Valid PublicUserEventParam param,
            HttpServletRequest request
    ) {
        String clientIp = extractClientIp(request);

        param.setUri(request.getRequestURI());
        param.setIp(clientIp);

        log.debug(
                "Public request to get events: uri={}, clientIp={}, params={}",
                request.getRequestURI(),
                clientIp,
                param
        );

        return eventService.getEventsForPublicRequests(param);
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(
            @PathVariable long id,
            HttpServletRequest request
    ) {
        String clientIp = extractClientIp(request);

        log.debug(
                "Public request to get event: uri={}, clientIp={}, eventId={}",
                request.getRequestURI(),
                clientIp,
                id
        );

        return eventService.findEventById(
                request.getRequestURI(),
                clientIp,
                id
        );
    }

    private String extractClientIp(HttpServletRequest request) {
        String gatewayClientIp =
                extractFirstAddress(request.getHeader(X_CLIENT_IP));

        if (gatewayClientIp != null) {
            return gatewayClientIp;
        }

        String forwardedIp =
                extractFirstAddress(request.getHeader(X_FORWARDED_FOR));

        if (forwardedIp != null) {
            return forwardedIp;
        }

        String realIp =
                extractFirstAddress(request.getHeader(X_REAL_IP));

        if (realIp != null) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private String extractFirstAddress(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        String firstAddress = headerValue.split(",")[0].trim();

        if (firstAddress.isBlank()
                || "unknown".equalsIgnoreCase(firstAddress)) {
            return null;
        }

        return firstAddress;
    }
}