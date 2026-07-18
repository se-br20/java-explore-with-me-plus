package ru.practicum.interaction.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailsDto {

    private Long id;

    private Long initiatorId;

    private EventStateDto state;

    private Integer participantLimit;

    private Boolean requestModeration;
}