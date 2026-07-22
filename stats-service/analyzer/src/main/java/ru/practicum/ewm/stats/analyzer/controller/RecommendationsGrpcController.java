package ru.practicum.ewm.stats.analyzer.controller;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.analyzer.dto.Recommendation;
import ru.practicum.ewm.stats.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.UserPredictionsRequestProto;

import java.util.List;
import java.util.function.Supplier;

@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventProto>
                    responseObserver
    ) {
        executeStreamingRequest(
                responseObserver,
                () -> {
                    validateRequest(
                            request,
                            "User predictions request"
                    );

                    return recommendationService
                            .getRecommendationsForUser(
                                    request.getUserId(),
                                    request.getMaxResults()
                            );
                }
        );
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto>
                    responseObserver
    ) {
        executeStreamingRequest(
                responseObserver,
                () -> {
                    validateRequest(
                            request,
                            "Similar events request"
                    );

                    return recommendationService
                            .getSimilarEvents(
                                    request.getEventId(),
                                    request.getUserId(),
                                    request.getMaxResults()
                            );
                }
        );
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventProto>
                    responseObserver
    ) {
        executeStreamingRequest(
                responseObserver,
                () -> {
                    validateRequest(
                            request,
                            "Interactions count request"
                    );

                    return recommendationService
                            .getInteractionsCount(
                                    request.getEventIdList()
                            );
                }
        );
    }

    private void executeStreamingRequest(
            StreamObserver<RecommendedEventProto>
                    responseObserver,

            Supplier<List<Recommendation>>
                    recommendationsSupplier
    ) {
        try {
            List<Recommendation> recommendations =
                    recommendationsSupplier.get();

            for (Recommendation recommendation
                    : recommendations) {

                responseObserver.onNext(
                        toProto(recommendation)
                );
            }

            responseObserver.onCompleted();

        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(
                                    exception.getMessage()
                            )
                            .withCause(exception)
                            .asRuntimeException()
            );

        } catch (Exception exception) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Failed to process "
                                            + "recommendation request"
                            )
                            .withCause(exception)
                            .asRuntimeException()
            );
        }
    }

    private RecommendedEventProto toProto(
            Recommendation recommendation
    ) {
        return RecommendedEventProto
                .newBuilder()
                .setEventId(
                        recommendation.eventId()
                )
                .setScore(
                        recommendation.score()
                )
                .build();
    }

    private void validateRequest(
            Object request,
            String requestName
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    requestName + " must not be null"
            );
        }
    }
}