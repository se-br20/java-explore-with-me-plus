package ru.practicum.ewm.request.dto;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@UtilityClass
public class ParticipationRequestMapper {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    public ParticipationRequest toParticipationRequest(
            Long eventId,
            Long requesterId
    ) {
        return ParticipationRequest.builder()
                .eventId(eventId)
                .requesterId(requesterId)
                .created(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();
    }

    public ParticipationRequestDto toParticipationRequestDto(
            ParticipationRequest request
    ) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(
                        request.getCreated().format(FORMATTER)
                )
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus())
                .build();
    }

    public List<ParticipationRequestDto> toParticipationRequestDto(
            List<ParticipationRequest> requests
    ) {
        return requests.stream()
                .map(
                        ParticipationRequestMapper
                                ::toParticipationRequestDto
                )
                .toList();
    }

    public EventRequestStatusUpdateResult
    toEventRequestStatusUpdateResult(
            List<ParticipationRequest> confirmedRequests,
            List<ParticipationRequest> rejectedRequests
    ) {
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(
                        toParticipationRequestDto(
                                confirmedRequests
                        )
                )
                .rejectedRequests(
                        toParticipationRequestDto(
                                rejectedRequests
                        )
                )
                .build();
    }
}