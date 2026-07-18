package ru.practicum.ewm.comments.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.interaction.event.EventDetailsDto;

@FeignClient(
        name = "event-service",
        path = "/internal/events",
        fallbackFactory = EventServiceClientFallbackFactory.class
)
public interface EventServiceClient {

    @GetMapping("/{eventId}")
    EventDetailsDto getEvent(
            @PathVariable("eventId") Long eventId
    );
}