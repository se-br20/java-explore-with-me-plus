package ru.practicum.ewm.stats.analyzer.controller;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import ru.practicum.ewm.stats.analyzer.dto.Recommendation;
import ru.practicum.ewm.stats.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.UserPredictionsRequestProto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationsGrpcControllerTest {

    private RecommendationService recommendationService;

    private RecommendationsGrpcController controller;

    private StreamObserver<RecommendedEventProto>
            responseObserver;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        recommendationService =
                mock(RecommendationService.class);

        controller =
                new RecommendationsGrpcController(
                        recommendationService
                );

        responseObserver =
                mock(StreamObserver.class);
    }

    @Test
    void shouldStreamRecommendationsForUser() {
        when(
                recommendationService
                        .getRecommendationsForUser(
                                1L,
                                2
                        )
        ).thenReturn(
                List.of(
                        new Recommendation(
                                10L,
                                0.9
                        ),
                        new Recommendation(
                                20L,
                                0.7
                        )
                )
        );

        UserPredictionsRequestProto request =
                UserPredictionsRequestProto
                        .newBuilder()
                        .setUserId(1L)
                        .setMaxResults(2)
                        .build();

        controller.getRecommendationsForUser(
                request,
                responseObserver
        );

        ArgumentCaptor<RecommendedEventProto>
                responseCaptor =
                ArgumentCaptor.forClass(
                        RecommendedEventProto.class
                );

        verify(
                responseObserver,
                org.mockito.Mockito.times(2)
        ).onNext(responseCaptor.capture());

        List<RecommendedEventProto> responses =
                responseCaptor.getAllValues();

        assertEquals(
                10L,
                responses.get(0).getEventId()
        );

        assertEquals(
                0.9,
                responses.get(0).getScore(),
                0.000001
        );

        assertEquals(
                20L,
                responses.get(1).getEventId()
        );

        assertEquals(
                0.7,
                responses.get(1).getScore(),
                0.000001
        );

        verify(responseObserver).onCompleted();
        verify(responseObserver, never())
                .onError(
                        org.mockito.ArgumentMatchers
                                .any()
                );
    }

    @Test
    void shouldStreamSimilarEvents() {
        when(
                recommendationService
                        .getSimilarEvents(
                                10L,
                                1L,
                                2
                        )
        ).thenReturn(
                List.of(
                        new Recommendation(
                                20L,
                                0.8
                        )
                )
        );

        SimilarEventsRequestProto request =
                SimilarEventsRequestProto
                        .newBuilder()
                        .setEventId(10L)
                        .setUserId(1L)
                        .setMaxResults(2)
                        .build();

        controller.getSimilarEvents(
                request,
                responseObserver
        );

        ArgumentCaptor<RecommendedEventProto>
                responseCaptor =
                ArgumentCaptor.forClass(
                        RecommendedEventProto.class
                );

        verify(responseObserver)
                .onNext(responseCaptor.capture());

        RecommendedEventProto response =
                responseCaptor.getValue();

        assertEquals(
                20L,
                response.getEventId()
        );

        assertEquals(
                0.8,
                response.getScore(),
                0.000001
        );

        verify(responseObserver).onCompleted();
    }

    @Test
    void shouldStreamInteractionCounts() {
        when(
                recommendationService
                        .getInteractionsCount(
                                List.of(
                                        10L,
                                        20L
                                )
                        )
        ).thenReturn(
                List.of(
                        new Recommendation(
                                10L,
                                2.2
                        ),
                        new Recommendation(
                                20L,
                                0.4
                        )
                )
        );

        InteractionsCountRequestProto request =
                InteractionsCountRequestProto
                        .newBuilder()
                        .addEventId(10L)
                        .addEventId(20L)
                        .build();

        controller.getInteractionsCount(
                request,
                responseObserver
        );

        ArgumentCaptor<RecommendedEventProto>
                responseCaptor =
                ArgumentCaptor.forClass(
                        RecommendedEventProto.class
                );

        verify(
                responseObserver,
                org.mockito.Mockito.times(2)
        ).onNext(responseCaptor.capture());

        List<RecommendedEventProto> responses =
                responseCaptor.getAllValues();

        assertEquals(
                10L,
                responses.get(0).getEventId()
        );

        assertEquals(
                2.2,
                responses.get(0).getScore(),
                0.000001
        );

        assertEquals(
                20L,
                responses.get(1).getEventId()
        );

        assertEquals(
                0.4,
                responses.get(1).getScore(),
                0.000001
        );

        verify(responseObserver).onCompleted();
    }

    @Test
    void shouldReturnInvalidArgumentStatus() {
        when(
                recommendationService
                        .getSimilarEvents(
                                10L,
                                0L,
                                5
                        )
        ).thenThrow(
                new IllegalArgumentException(
                        "userId must be positive"
                )
        );

        SimilarEventsRequestProto request =
                SimilarEventsRequestProto
                        .newBuilder()
                        .setEventId(10L)
                        .setUserId(0L)
                        .setMaxResults(5)
                        .build();

        controller.getSimilarEvents(
                request,
                responseObserver
        );

        ArgumentCaptor<Throwable> errorCaptor =
                ArgumentCaptor.forClass(
                        Throwable.class
                );

        verify(responseObserver)
                .onError(errorCaptor.capture());

        Status status =
                Status.fromThrowable(
                        errorCaptor.getValue()
                );

        assertEquals(
                Status.Code.INVALID_ARGUMENT,
                status.getCode()
        );

        assertEquals(
                "userId must be positive",
                status.getDescription()
        );

        verify(responseObserver, never())
                .onCompleted();

        verify(responseObserver, never())
                .onNext(
                        org.mockito.ArgumentMatchers
                                .any()
                );
    }

    @Test
    void shouldReturnInternalStatusForUnexpectedError() {
        when(
                recommendationService
                        .getRecommendationsForUser(
                                1L,
                                5
                        )
        ).thenThrow(
                new IllegalStateException(
                        "Database is unavailable"
                )
        );

        UserPredictionsRequestProto request =
                UserPredictionsRequestProto
                        .newBuilder()
                        .setUserId(1L)
                        .setMaxResults(5)
                        .build();

        controller.getRecommendationsForUser(
                request,
                responseObserver
        );

        ArgumentCaptor<Throwable> errorCaptor =
                ArgumentCaptor.forClass(
                        Throwable.class
                );

        verify(responseObserver)
                .onError(errorCaptor.capture());

        Status status =
                Status.fromThrowable(
                        errorCaptor.getValue()
                );

        assertEquals(
                Status.Code.INTERNAL,
                status.getCode()
        );

        assertEquals(
                "Failed to process recommendation request",
                status.getDescription()
        );

        verify(responseObserver, never())
                .onCompleted();
    }

    @Test
    void shouldSendResponsesBeforeCompletion() {
        when(
                recommendationService
                        .getRecommendationsForUser(
                                1L,
                                1
                        )
        ).thenReturn(
                List.of(
                        new Recommendation(
                                10L,
                                0.9
                        )
                )
        );

        UserPredictionsRequestProto request =
                UserPredictionsRequestProto
                        .newBuilder()
                        .setUserId(1L)
                        .setMaxResults(1)
                        .build();

        controller.getRecommendationsForUser(
                request,
                responseObserver
        );

        InOrder order = inOrder(responseObserver);

        order.verify(responseObserver)
                .onNext(
                        org.mockito.ArgumentMatchers
                                .any()
                );

        order.verify(responseObserver)
                .onCompleted();
    }
}