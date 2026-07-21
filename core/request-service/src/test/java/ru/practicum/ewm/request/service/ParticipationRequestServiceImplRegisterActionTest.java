package ru.practicum.ewm.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ru.practicum.ewm.request.client.EventServiceClient;
import ru.practicum.ewm.request.client.UserServiceClient;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.interaction.event.EventDetailsDto;
import ru.practicum.interaction.event.EventStateDto;
import ru.practicum.interaction.user.UserDetailsDto;
import ru.practicum.stat.client.CollectorClient;
import ru.practicum.stat.client.UserActionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticipationRequestServiceImplRegisterActionTest {

    private ParticipationRequestRepository requestRepository;
    private UserServiceClient userServiceClient;
    private EventServiceClient eventServiceClient;
    private CollectorClient collectorClient;

    private ParticipationRequestServiceImpl requestService;

    @BeforeEach
    void setUp() {
        requestRepository =
                mock(ParticipationRequestRepository.class);

        userServiceClient =
                mock(UserServiceClient.class);

        eventServiceClient =
                mock(EventServiceClient.class);

        collectorClient =
                mock(CollectorClient.class);

        requestService =
                new ParticipationRequestServiceImpl(
                        requestRepository,
                        userServiceClient,
                        eventServiceClient,
                        collectorClient
                );
    }

    @Test
    void shouldSendRegisterAfterSuccessfulRequestCreation() {
        long userId = 5L;
        long eventId = 10L;

        UserDetailsDto user =
                mock(UserDetailsDto.class);

        EventDetailsDto event =
                mock(EventDetailsDto.class);

        when(userServiceClient.getUser(userId))
                .thenReturn(user);

        when(eventServiceClient.getEvent(eventId))
                .thenReturn(event);

        when(event.getInitiatorId())
                .thenReturn(99L);

        when(event.getState())
                .thenReturn(EventStateDto.PUBLISHED);

        when(event.getParticipantLimit())
                .thenReturn(0);

        when(event.getRequestModeration())
                .thenReturn(false);

        when(
                requestRepository
                        .existsByEventIdAndRequesterId(
                                eventId,
                                userId
                        )
        ).thenReturn(false);

        when(
                requestRepository.save(
                        any(ParticipationRequest.class)
                )
        ).thenAnswer(invocation -> {
            ParticipationRequest request =
                    invocation.getArgument(0);

            request.setId(1L);

            return request;
        });

        ParticipationRequestDto result =
                requestService.createRequest(
                        userId,
                        eventId
                );

        assertEquals(1L, result.getId());
        assertEquals(userId, result.getRequester());
        assertEquals(eventId, result.getEvent());
        assertEquals(
                RequestStatus.CONFIRMED,
                result.getStatus()
        );

        verify(collectorClient).sendAction(
                userId,
                eventId,
                UserActionType.REGISTER
        );
    }

    @Test
    void shouldNotSendRegisterWhenSavingRequestFails() {
        long userId = 5L;
        long eventId = 10L;

        EventDetailsDto event =
                mock(EventDetailsDto.class);

        when(userServiceClient.getUser(userId))
                .thenReturn(
                        mock(UserDetailsDto.class)
                );

        when(eventServiceClient.getEvent(eventId))
                .thenReturn(event);

        when(event.getInitiatorId())
                .thenReturn(99L);

        when(event.getState())
                .thenReturn(EventStateDto.PUBLISHED);

        when(event.getParticipantLimit())
                .thenReturn(0);

        when(event.getRequestModeration())
                .thenReturn(false);

        when(
                requestRepository
                        .existsByEventIdAndRequesterId(
                                eventId,
                                userId
                        )
        ).thenReturn(false);

        when(
                requestRepository.save(
                        any(ParticipationRequest.class)
                )
        ).thenThrow(
                new RuntimeException("Database failure")
        );

        assertThrows(
                RuntimeException.class,
                () -> requestService.createRequest(
                        userId,
                        eventId
                )
        );

        verify(collectorClient, never())
                .sendAction(
                        anyLong(),
                        anyLong(),
                        any(UserActionType.class)
                );
    }

    @Test
    void shouldReturnTrueForConfirmedRequest() {
        long userId = 5L;
        long eventId = 10L;

        when(
                requestRepository
                        .existsByEventIdAndRequesterIdAndStatus(
                                eventId,
                                userId,
                                RequestStatus.CONFIRMED
                        )
        ).thenReturn(true);

        boolean result =
                requestService.hasConfirmedRequest(
                        userId,
                        eventId
                );

        assertTrue(result);

        verify(requestRepository)
                .existsByEventIdAndRequesterIdAndStatus(
                        eventId,
                        userId,
                        RequestStatus.CONFIRMED
                );
    }
}
