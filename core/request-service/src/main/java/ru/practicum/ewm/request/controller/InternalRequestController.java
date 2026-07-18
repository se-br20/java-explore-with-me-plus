package ru.practicum.ewm.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.request.service.ParticipationRequestService;
import ru.practicum.interaction.request.ConfirmedRequestsResponse;
import ru.practicum.interaction.request.EventIdsRequest;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final ParticipationRequestService requestService;

    @PostMapping("/confirmed-counts")
    public ConfirmedRequestsResponse
    getConfirmedRequestCounts(
            @Valid @RequestBody EventIdsRequest request
    ) {
        return requestService.getConfirmedRequestCounts(
                request
        );
    }
}