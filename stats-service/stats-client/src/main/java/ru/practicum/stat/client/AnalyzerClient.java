package ru.practicum.stat.client;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AnalyzerClient {

    private static final long DEADLINE_SECONDS = 5L;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc
            .RecommendationsControllerBlockingStub analyzerStub;

    public List<RecommendedEvent>
    getRecommendationsForUser(
            long userId,
            int maxResults
    ) {
        validatePositiveId(userId, "userId");
        validateMaxResults(maxResults);

        UserPredictionsRequestProto request =
                UserPredictionsRequestProto
                        .newBuilder()
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build();

        try {
            Iterator<RecommendedEventProto> response =
                    analyzerStub
                            .withWaitForReady()
                            .withDeadlineAfter(
                                    DEADLINE_SECONDS,
                                    TimeUnit.SECONDS
                            )
                            .getRecommendationsForUser(
                                    request
                            );

            return toList(response);

        } catch (StatusRuntimeException exception) {
            log.warn(
                    "Analyzer recommendations request "
                            + "failed: userId={}, status={}",
                    userId,
                    exception.getStatus()
            );

            return List.of();
        }
    }

    public List<RecommendedEvent> getSimilarEvents(
            long eventId,
            long userId,
            int maxResults
    ) {
        validatePositiveId(eventId, "eventId");
        validatePositiveId(userId, "userId");
        validateMaxResults(maxResults);

        SimilarEventsRequestProto request =
                SimilarEventsRequestProto
                        .newBuilder()
                        .setEventId(eventId)
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build();

        try {
            Iterator<RecommendedEventProto> response =
                    analyzerStub
                            .withWaitForReady()
                            .withDeadlineAfter(
                                    DEADLINE_SECONDS,
                                    TimeUnit.SECONDS
                            )
                            .getSimilarEvents(request);

            return toList(response);

        } catch (StatusRuntimeException exception) {
            log.warn(
                    "Analyzer similar events request failed: "
                            + "eventId={}, userId={}, status={}",
                    eventId,
                    userId,
                    exception.getStatus()
            );

            return List.of();
        }
    }

    public List<RecommendedEvent> getInteractionsCount(
            Collection<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> uniqueEventIds =
                new LinkedHashSet<>();

        for (Long eventId : eventIds) {
            if (eventId == null) {
                throw new IllegalArgumentException(
                        "eventId must not be null"
                );
            }

            validatePositiveId(
                    eventId,
                    "eventId"
            );

            uniqueEventIds.add(eventId);
        }

        InteractionsCountRequestProto request =
                InteractionsCountRequestProto
                        .newBuilder()
                        .addAllEventId(
                                uniqueEventIds
                        )
                        .build();

        try {
            Iterator<RecommendedEventProto> response =
                    analyzerStub
                            .withWaitForReady()
                            .withDeadlineAfter(
                                    DEADLINE_SECONDS,
                                    TimeUnit.SECONDS
                            )
                            .getInteractionsCount(request);

            return toList(response);

        } catch (StatusRuntimeException exception) {
            log.warn(
                    "Analyzer interaction count request "
                            + "failed: eventIds={}, status={}",
                    uniqueEventIds,
                    exception.getStatus()
            );

            return List.of();
        }
    }

    private List<RecommendedEvent> toList(
            Iterator<RecommendedEventProto> iterator
    ) {
        List<RecommendedEvent> result =
                new ArrayList<>();

        iterator.forEachRemaining(
                response ->
                        result.add(
                                new RecommendedEvent(
                                        response.getEventId(),
                                        response.getScore()
                                )
                        )
        );

        return List.copyOf(result);
    }

    private void validatePositiveId(
            long id,
            String fieldName
    ) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be positive"
            );
        }
    }

    private void validateMaxResults(
            int maxResults
    ) {
        if (maxResults <= 0) {
            throw new IllegalArgumentException(
                    "maxResults must be positive"
            );
        }
    }
}