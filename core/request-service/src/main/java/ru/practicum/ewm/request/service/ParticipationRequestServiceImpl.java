package ru.practicum.ewm.request.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.exceptions.exceptions.ServiceUnavailableException;
import ru.practicum.ewm.request.client.EventServiceClient;
import ru.practicum.ewm.request.client.UserServiceClient;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.ParticipationRequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.interaction.event.EventDetailsDto;
import ru.practicum.interaction.event.EventStateDto;
import ru.practicum.interaction.request.ConfirmedRequestsResponse;
import ru.practicum.interaction.request.EventIdsRequest;
import ru.practicum.interaction.user.UserDetailsDto;

import ru.practicum.stat.client.CollectorClient;
import ru.practicum.stat.client.UserActionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl
        implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;
    private final CollectorClient collectorClient;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(
            Long userId,
            Long eventId
    ) {

        getUser(userId);
        EventDetailsDto event = getEvent(eventId);

        if (requestRepository.existsByEventIdAndRequesterId(
                eventId,
                userId
        )) {
            throw new ConditionsNotMetException(
                    "Request already exists for this event"
            );
        }

        if (event.getInitiatorId().equals(userId)) {
            throw new ConditionsNotMetException(
                    "Initiator cannot add a request "
                            + "to their own event"
            );
        }


        if (event.getState() != EventStateDto.PUBLISHED) {
            throw new ConditionsNotMetException(
                    "Impossible to add a request "
                            + "to an unpublished event"
            );
        }

        int participantLimit =
                event.getParticipantLimit() != null
                        ? event.getParticipantLimit()
                        : 0;

        if (participantLimit > 0) {
            long confirmedRequests =
                    requestRepository
                            .countByEventIdAndStatus(
                                    eventId,
                                    RequestStatus.CONFIRMED
                            );

            if (confirmedRequests >= participantLimit) {
                throw new ConditionsNotMetException(
                        "The participant limit "
                                + "has been reached"
                );
            }
        }

        ParticipationRequest request =
                ParticipationRequestMapper.toParticipationRequest(
                        eventId,
                        userId
                );

        boolean moderationEnabled =
                !Boolean.FALSE.equals(
                        event.getRequestModeration()
                );

        if (participantLimit == 0 || !moderationEnabled) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        request = requestRepository.save(request);

        collectorClient.sendAction(
                userId,
                eventId,
                UserActionType.REGISTER
        );

        return ParticipationRequestMapper.toParticipationRequestDto(
                request
        );
    }

    @Override
    public List<ParticipationRequestDto>
    getUserParticipationRequests(Long userId) {
        getUser(userId);

        List<ParticipationRequest> requests =
                requestRepository.findByRequesterId(userId);

        return ParticipationRequestMapper.toParticipationRequestDto(
                requests
        );
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(
            Long userId,
            Long requestId
    ) {
        getUser(userId);

        ParticipationRequest request =
                requestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Request with id="
                                                + requestId
                                                + " was not found"
                                )
                        );

        if (!request.getRequesterId().equals(userId)) {
            throw new NotFoundException(
                    "Request with id="
                            + requestId
                            + " was not found for user with id="
                            + userId
            );
        }

        if (request.getStatus()
                != RequestStatus.PENDING
                && request.getStatus()
                != RequestStatus.CONFIRMED) {

            throw new ConditionsNotMetException(
                    "Only PENDING or CONFIRMED requests "
                            + "can be canceled"
            );
        }

        request.setStatus(RequestStatus.CANCELED);

        return ParticipationRequestMapper.toParticipationRequestDto(
                requestRepository.save(request)
        );
    }

    @Override
    public List<ParticipationRequestDto>
    getParticipationRequests(
            Long userId,
            Long eventId
    ) {
        getUser(userId);
        EventDetailsDto event = getEvent(eventId);

        verifyEventInitiator(
                event,
                userId
        );

        List<ParticipationRequest> requests =
                requestRepository.findByEventId(eventId);

        return ParticipationRequestMapper
                .toParticipationRequestDto(requests);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult
    updateRequestStatuses(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest updateRequest
    ) {
        getUser(userId);
        EventDetailsDto event = getEvent(eventId);

        verifyEventInitiator(
                event,
                userId
        );

        RequestStatus requestedStatus =
                updateRequest.getStatus();

        if (requestedStatus != RequestStatus.CONFIRMED
                && requestedStatus
                != RequestStatus.REJECTED) {

            throw new ConditionsNotMetException(
                    "Status must be CONFIRMED or REJECTED"
            );
        }

        List<Long> requestIds =
                updateRequest.getRequestIds()
                        .stream()
                        .distinct()
                        .toList();

        List<ParticipationRequest> requests =
                requestRepository.findByIdIn(requestIds);

        if (requests.size() != requestIds.size()) {
            throw new NotFoundException(
                    "One or more participation requests "
                            + "were not found"
            );
        }

        for (ParticipationRequest request : requests) {
            if (request.getStatus()
                    != RequestStatus.PENDING) {

                throw new ConditionsNotMetException(
                        "Only PENDING requests "
                                + "can be reviewed"
                );
            }

            if (!request.getEventId().equals(eventId)) {
                throw new ConditionsNotMetException(
                        "One or more requests do not belong "
                                + "to event with id="
                                + eventId
                );
            }
        }

        List<ParticipationRequest> confirmed =
                new ArrayList<>();

        List<ParticipationRequest> rejected =
                new ArrayList<>();

        if (requestedStatus == RequestStatus.REJECTED) {
            rejected.addAll(requests);

            updateStatuses(
                    rejected,
                    RequestStatus.REJECTED
            );

            return ParticipationRequestMapper
                    .toEventRequestStatusUpdateResult(
                            confirmed,
                            rejected
                    );
        }

        int participantLimit =
                event.getParticipantLimit() != null
                        ? event.getParticipantLimit()
                        : 0;

        boolean moderationEnabled =
                !Boolean.FALSE.equals(
                        event.getRequestModeration()
                );

        if (participantLimit == 0 || !moderationEnabled) {
            confirmed.addAll(requests);

            updateStatuses(
                    confirmed,
                    RequestStatus.CONFIRMED
            );

            return ParticipationRequestMapper
                    .toEventRequestStatusUpdateResult(
                            confirmed,
                            rejected
                    );
        }

        long confirmedCount =
                requestRepository.countByEventIdAndStatus(
                        eventId,
                        RequestStatus.CONFIRMED
                );

        if (confirmedCount >= participantLimit) {
            throw new ConditionsNotMetException(
                    "The participant limit has been reached"
            );
        }

        for (ParticipationRequest request : requests) {
            if (confirmedCount < participantLimit) {
                confirmed.add(request);
                confirmedCount++;
            } else {
                rejected.add(request);
            }
        }

        updateStatuses(
                confirmed,
                RequestStatus.CONFIRMED
        );

        updateStatuses(
                rejected,
                RequestStatus.REJECTED
        );

        if (confirmedCount >= participantLimit) {
            requestRepository.updateStatusByEventId(
                    eventId,
                    RequestStatus.PENDING,
                    RequestStatus.REJECTED
            );
        }

        return ParticipationRequestMapper
                .toEventRequestStatusUpdateResult(
                        confirmed,
                        rejected
                );
    }

    @Override
    public ConfirmedRequestsResponse
    getConfirmedRequestCounts(
            EventIdsRequest request
    ) {
        if (request == null
                || request.getEventIds() == null
                || request.getEventIds().isEmpty()) {

            return ConfirmedRequestsResponse.builder()
                    .confirmedRequests(Map.of())
                    .build();
        }

        List<Long> eventIds =
                request.getEventIds()
                        .stream()
                        .distinct()
                        .toList();

        Map<Long, Long> counts =
                new LinkedHashMap<>();

        eventIds.forEach(
                eventId -> counts.put(eventId, 0L)
        );

        List<Object[]> rows =
                requestRepository.countByEventIdsAndStatus(
                        eventIds,
                        RequestStatus.CONFIRMED
                );

        for (Object[] row : rows) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];

            counts.put(eventId, count);
        }

        return ConfirmedRequestsResponse.builder()
                .confirmedRequests(counts)
                .build();
    }

    @Override
    public boolean hasConfirmedRequest(
            Long userId,
            Long eventId
    ) {
        if (userId == null || eventId == null) {
            return false;
        }

        return requestRepository
                .existsByEventIdAndRequesterIdAndStatus(
                        eventId,
                        userId,
                        RequestStatus.CONFIRMED
                );
    }

    private void verifyEventInitiator(
            EventDetailsDto event,
            Long userId
    ) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException(
                    "Event with id="
                            + event.getId()
                            + " was not found for user with id="
                            + userId
            );
        }
    }

    private void updateStatuses(
            List<ParticipationRequest> requests,
            RequestStatus status
    ) {
        if (requests.isEmpty()) {
            return;
        }

        List<Long> ids = requests.stream()
                .map(ParticipationRequest::getId)
                .toList();

        int updated =
                requestRepository.updateStatusByIdIn(
                        ids,
                        status
                );

        if (updated != ids.size()) {
            throw new IllegalStateException(
                    "Failed to update all requests. "
                            + "Expected="
                            + ids.size()
                            + ", updated="
                            + updated
            );
        }

        requests.forEach(
                request -> request.setStatus(status)
        );
    }

    private UserDetailsDto getUser(Long userId) {
        try {
            return userServiceClient.getUser(userId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException(
                    "User with id=" + userId + " was not found"
            );
        } catch (FeignException exception) {
            log.error(
                    "Failed to get user with id={} from user-service",
                    userId,
                    exception
            );

            throw new ServiceUnavailableException(
                    "user-service is unavailable",
                    exception
            );
        }
    }

    private EventDetailsDto getEvent(Long eventId) {
        try {
            return eventServiceClient.getEvent(eventId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException(
                    "Event with id="
                            + eventId
                            + " was not found"
            );
        } catch (FeignException exception) {
            log.error(
                    "Failed to get event with id={} from event-service",
                    eventId,
                    exception
            );

            throw new ServiceUnavailableException(
                    "event-service is unavailable",
                    exception
            );
        }
    }
}