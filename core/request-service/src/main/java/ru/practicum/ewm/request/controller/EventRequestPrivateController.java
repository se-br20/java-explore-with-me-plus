package ru.practicum.ewm.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(
        "/users/{userId}/events/{eventId}/requests"
)
@RequiredArgsConstructor
public class EventRequestPrivateController {

    private final ParticipationRequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto>
    getParticipationRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId
    ) {
        log.debug(
                "Getting requests: userId={}, eventId={}",
                userId,
                eventId
        );

        return requestService.getParticipationRequests(
                userId,
                eventId
        );
    }

    @PatchMapping
    public EventRequestStatusUpdateResult
    updateRequestStatuses(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody
            EventRequestStatusUpdateRequest request
    ) {
        log.debug(
                "Updating request statuses: "
                        + "userId={}, eventId={}",
                userId,
                eventId
        );

        return requestService.updateRequestStatuses(
                userId,
                eventId,
                request
        );
    }
}