package ru.practicum.ewm.event.service;

import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.dto.paramDto.AdminUserEventParam;
import ru.practicum.ewm.event.dto.paramDto.PublicUserEventParam;

import java.util.Collection;
import java.util.List;

public interface EventService {

    EventFullDto createEvent(
            Long userId,
            NewEventDto newEventDto
    );

    List<EventShortDto> getUserEvents(
            Long userId,
            int from,
            int size
    );

    List<EventShortDto> getEventsForPublicRequests(
            PublicUserEventParam userEventParam
    );

    List<EventShortDto> getRecommendations(
            Long userId
    );

    void likeEvent(
            Long userId,
            Long eventId
    );

    List<EventFullDto> getEventsForAdminRequests(
            AdminUserEventParam adminParam
    );

    EventFullDto findUserEventByEventId(
            Long userId,
            Long eventId
    );

    EventFullDto updateUserEvent(
            Long userId,
            Long eventId,
            UpdateEventUserRequest body
    );

    EventFullDto updateEvent(
            Long eventId,
            UpdateEventAdminRequest body
    );

    EventFullDto findEventById(
            String uri,
            String ip,
            Long id,
            Long userId
    );

    List<EventShortDto> getShortDtosByIds(
            Collection<Long> eventIds
    );
}