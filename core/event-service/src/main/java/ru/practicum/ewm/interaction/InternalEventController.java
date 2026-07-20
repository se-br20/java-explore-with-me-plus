package ru.practicum.ewm.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction.event.EventDetailsDto;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final InternalEventService internalEventService;

    @GetMapping("/{eventId}")
    public EventDetailsDto getEvent(
            @PathVariable Long eventId
    ) {
        return internalEventService.getEvent(eventId);
    }
}