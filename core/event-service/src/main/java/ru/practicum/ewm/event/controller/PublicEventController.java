package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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

    private static final String X_FORWARDED_FOR =
            "X-Forwarded-For";

    private static final String X_REAL_IP =
            "X-Real-IP";

    private static final String X_EWM_USER_ID =
            "X-EWM-USER-ID";

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(
            @Valid PublicUserEventParam param,
            HttpServletRequest request
    ) {
        String clientIp =
                extractClientIp(request);

        param.setUri(
                request.getRequestURI()
        );

        param.setIp(clientIp);

        log.debug(
                "Public request to get events: "
                        + "uri={}, clientIp={}, params={}",
                request.getRequestURI(),
                clientIp,
                param
        );

        return eventService
                .getEventsForPublicRequests(param);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader(X_EWM_USER_ID)
            @Positive(
                    message =
                            "User id must be positive"
            )
            Long userId
    ) {
        log.debug(
                "Public request to get recommendations: "
                        + "userId={}",
                userId
        );

        return eventService
                .getRecommendations(userId);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(
            @PathVariable Long eventId,

            @RequestHeader(X_EWM_USER_ID)
            @Positive(
                    message =
                            "User id must be positive"
            )
            Long userId
    ) {
        log.debug(
                "Public request to like event: "
                        + "userId={}, eventId={}",
                userId,
                eventId
        );

        eventService.likeEvent(
                userId,
                eventId
        );
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(
            @PathVariable long id,

            @RequestHeader(
                    value = X_EWM_USER_ID,
                    required = false
            )
            @Positive(
                    message =
                            "User id must be positive"
            )
            Long userId,

            HttpServletRequest request
    ) {
        String clientIp =
                extractClientIp(request);

        log.debug(
                "Public request to get event: "
                        + "uri={}, clientIp={}, "
                        + "eventId={}, userId={}",
                request.getRequestURI(),
                clientIp,
                id,
                userId
        );

        return eventService.findEventById(
                request.getRequestURI(),
                clientIp,
                id,
                userId
        );
    }

    private String extractClientIp(
            HttpServletRequest request
    ) {
        String forwardedFor =
                request.getHeader(
                        X_FORWARDED_FOR
                );

        if (forwardedFor != null
                && !forwardedFor.isBlank()) {

            return forwardedFor
                    .split(",")[0]
                    .trim();
        }

        String realIp =
                request.getHeader(
                        X_REAL_IP
                );

        if (realIp != null
                && !realIp.isBlank()) {

            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}