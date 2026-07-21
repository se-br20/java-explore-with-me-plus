package ru.practicum.ewm.request.service;

import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.interaction.request.ConfirmedRequestsResponse;
import ru.practicum.interaction.request.EventIdsRequest;

import java.util.List;

public interface ParticipationRequestService {

    ParticipationRequestDto createRequest(
            Long userId,
            Long eventId
    );

    List<ParticipationRequestDto>
    getUserParticipationRequests(
            Long userId
    );

    ParticipationRequestDto cancelRequest(
            Long userId,
            Long requestId
    );

    List<ParticipationRequestDto>
    getParticipationRequests(
            Long userId,
            Long eventId
    );

    EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest request
    );

    ConfirmedRequestsResponse getConfirmedRequestCounts(
            EventIdsRequest request
    );

    boolean hasConfirmedRequest(
            Long userId,
            Long eventId
    );
}