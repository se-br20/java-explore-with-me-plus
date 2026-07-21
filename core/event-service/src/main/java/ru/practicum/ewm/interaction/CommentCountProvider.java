package ru.practicum.ewm.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.event.dto.EventCountsAware;
import ru.practicum.ewm.interaction.client.CommentServiceClient;
import ru.practicum.interaction.comment.ApprovedCommentsResponse;
import ru.practicum.interaction.comment.CommentEventIdsRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommentCountProvider {

    private final CommentServiceClient commentServiceClient;

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
                eventDto.setCommentsCount(
                        counts.getOrDefault(
                                eventDto.getId(),
                                0L
                        )
                )
        );
    }

    private Map<Long, Long> getCounts(
            List<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        CommentEventIdsRequest request =
                CommentEventIdsRequest.builder()
                        .eventIds(eventIds)
                        .build();

        ApprovedCommentsResponse response =
                commentServiceClient
                        .getApprovedCommentCounts(request);

        if (response == null
                || response.getApprovedComments() == null) {

            return Collections.emptyMap();
        }

        return response.getApprovedComments();
    }
}