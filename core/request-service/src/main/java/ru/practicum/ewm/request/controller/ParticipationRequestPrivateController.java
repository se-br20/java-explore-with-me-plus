package ru.practicum.ewm.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class ParticipationRequestPrivateController {

    private final ParticipationRequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId
    ) {
        log.debug(
                "Creating request: userId={}, eventId={}",
                userId,
                eventId
        );

        return requestService.createRequest(
                userId,
                eventId
        );
    }

    @GetMapping
    public List<ParticipationRequestDto>
    getUserParticipationRequests(
            @PathVariable Long userId
    ) {
        log.debug(
                "Getting user requests: userId={}",
                userId
        );

        return requestService
                .getUserParticipationRequests(userId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId
    ) {
        log.debug(
                "Canceling request: "
                        + "userId={}, requestId={}",
                userId,
                requestId
        );

        return requestService.cancelRequest(
                userId,
                requestId
        );
    }
}