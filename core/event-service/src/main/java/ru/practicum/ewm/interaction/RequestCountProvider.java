package ru.practicum.ewm.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.event.dto.EventCountsAware;
import ru.practicum.ewm.interaction.client.RequestServiceClient;
import ru.practicum.interaction.request.ConfirmedRequestsResponse;
import ru.practicum.interaction.request.EventIdsRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RequestCountProvider {

    private final RequestServiceClient requestServiceClient;

    public void enrich(
            List<? extends EventCountsAware> eventDtos
    ) {
        if (eventDtos == null || eventDtos.isEmpty()) {
            return;
        }

        List<Long> eventIds = eventDtos.stream()
                .map(EventCountsAware::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Long> counts = getCounts(eventIds);

        eventDtos.forEach(eventDto ->
                eventDto.setConfirmedRequests(
                        counts.getOrDefault(
                                eventDto.getId(),
                                0L
                        )
                )
        );
    }

    public long getConfirmedRequests(Long eventId) {
        if (eventId == null) {
            return 0L;
        }

        return getCounts(List.of(eventId))
                .getOrDefault(eventId, 0L);
    }

    public boolean hasConfirmedRequest(
            Long userId,
            Long eventId
    ) {
        if (userId == null || eventId == null) {
            return false;
        }

        return requestServiceClient.hasConfirmedRequest(
                userId,
                eventId
        );
    }

    private Map<Long, Long> getCounts(
            Collection<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        EventIdsRequest request = EventIdsRequest.builder()
                .eventIds(List.copyOf(eventIds))
                .build();

        ConfirmedRequestsResponse response =
                requestServiceClient.getConfirmedRequestCounts(
                        request
                );

        if (response == null
                || response.getConfirmedRequests() == null) {

            return Collections.emptyMap();
        }

        return response.getConfirmedRequests();
    }
}