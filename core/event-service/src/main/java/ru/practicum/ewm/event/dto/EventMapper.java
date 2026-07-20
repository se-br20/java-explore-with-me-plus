package ru.practicum.ewm.event.dto;

import ru.practicum.ewm.categories.dto.CategoryMapper;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.interaction.user.UserDetailsDto;
import ru.practicum.interaction.user.UserShortDto;

import java.time.LocalDateTime;

public final class EventMapper {

    private EventMapper() {
    }

    public static Event toEvent(
            NewEventDto newEventDto,
            Category category,
            UserDetailsDto initiator
    ) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(newEventDto.getLocation())
                .paid(
                        newEventDto.getPaid() != null
                                ? newEventDto.getPaid()
                                : false
                )
                .participantLimit(
                        newEventDto.getParticipantLimit() != null
                                ? newEventDto
                                .getParticipantLimit()
                                : 0
                )
                .requestModeration(
                        newEventDto.getRequestModeration()
                                != null
                                ? newEventDto
                                .getRequestModeration()
                                : true
                )
                .title(newEventDto.getTitle())
                .initiatorId(initiator.getId())
                .initiatorName(initiator.getName())
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
    }

    public static void updateEventFromAdminRequest(
            UpdateEventAdminRequest request,
            Event event,
            Category category
    ) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (category != null) {
            event.setCategory(category);
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }

        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(
                    request.getParticipantLimit()
            );
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(
                    request.getRequestModeration()
            );
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }

    public static void updateEventFromUserRequest(
            UpdateEventUserRequest request,
            Event event,
            Category category
    ) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (category != null) {
            event.setCategory(category);
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }

        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(
                    request.getParticipantLimit()
            );
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(
                    request.getRequestModeration()
            );
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }

    public static EventFullDto toEventFullDto(
            Event event,
            Long confirmedRequests,
            Long views
    ) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(
                        CategoryMapper.toCategoryDto(
                                event.getCategory()
                        )
                )
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(toInitiatorDto(event))
                .location(event.getLocation())
                .paid(event.getPaid())
                .participantLimit(
                        event.getParticipantLimit()
                )
                .publishedOn(event.getPublishedOn())
                .requestModeration(
                        event.getRequestModeration()
                )
                .state(event.getState())
                .title(event.getTitle())
                .views(views)
                .commentsCount(0L)
                .build();
    }

    public static EventShortDto toEventShortDto(
            Event event,
            Long confirmedRequests,
            Long views
    ) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(
                        CategoryMapper.toCategoryDto(
                                event.getCategory()
                        )
                )
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .initiator(toInitiatorDto(event))
                .paid(event.getPaid())
                .publishedOn(event.getPublishedOn())
                .title(event.getTitle())
                .views(views)
                .commentsCount(0L)
                .build();
    }

    private static UserShortDto toInitiatorDto(
            Event event
    ) {
        return UserShortDto.builder()
                .id(event.getInitiatorId())
                .name(event.getInitiatorName())
                .build();
    }
}