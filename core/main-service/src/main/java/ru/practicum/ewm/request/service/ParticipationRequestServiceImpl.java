package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.ParticipationRequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl
        implements ParticipationRequestService {

    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(
            Long userId,
            Long eventId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "User with id=" + userId + " was not found"
                ));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + eventId + " was not found"
                ));

        if (requestRepository.existsByEventIdAndRequesterId(
                eventId,
                userId
        )) {
            throw new ConditionsNotMetException(
                    "Request already exists for this event"
            );
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConditionsNotMetException(
                    "Initiator cannot participate in their own event"
            );
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConditionsNotMetException(
                    "Cannot participate in an unpublished event"
            );
        }

        if (event.getParticipantLimit() > 0) {
            long participants =
                    requestRepository.countByEventIdAndStatus(
                            eventId,
                            RequestStatus.CONFIRMED
                    );

            if (participants >= event.getParticipantLimit()) {
                throw new ConditionsNotMetException(
                        "The participant limit has been reached"
                );
            }
        }

        ParticipationRequest request =
                ParticipationRequestMapper.toParticipationRequest(
                        event,
                        user
                );

        if (event.getParticipantLimit() == 0
                || !event.getRequestModeration()) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        request = requestRepository.save(request);

        return ParticipationRequestMapper
                .toParticipationRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getUserParticipationRequests(
            Long userId
    ) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(
                    "User with id=" + userId + " was not found"
            );
        }

        return ParticipationRequestMapper.toParticipationRequestDto(
                requestRepository.findByRequesterId(userId)
        );
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(
            Long userId,
            Long requestId
    ) {
        ParticipationRequest request =
                requestRepository.findById(requestId)
                        .orElseThrow(() -> new NotFoundException(
                                "Request with id=" + requestId
                                        + " was not found"
                        ));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException(
                    "Request with id=" + requestId
                            + " not found for user with id=" + userId
            );
        }

        request.setStatus(RequestStatus.CANCELED);

        return ParticipationRequestMapper.toParticipationRequestDto(
                requestRepository.save(request)
        );
    }
}
