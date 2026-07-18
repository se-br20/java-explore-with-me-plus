package ru.practicum.ewm.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;
import ru.practicum.interaction.event.EventDetailsDto;
import ru.practicum.interaction.event.EventStateDto;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalEventService {

    private final EventRepository eventRepository;

    public EventDetailsDto getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Event with id="
                                        + eventId
                                        + " was not found"
                        )
                );

        return EventDetailsDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .state(
                        EventStateDto.valueOf(
                                event.getState().name()
                        )
                )
                .participantLimit(
                        event.getParticipantLimit()
                )
                .requestModeration(
                        event.getRequestModeration()
                )
                .build();
    }
}