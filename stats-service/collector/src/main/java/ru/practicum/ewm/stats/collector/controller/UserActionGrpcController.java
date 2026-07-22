package ru.practicum.ewm.stats.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.mapper.UserActionMapper;
import ru.practicum.ewm.stats.collector.service.UserActionProducer;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionProducer producer;

    @Override
    public void collectUserAction(
            UserActionProto request,
            StreamObserver<Empty> responseObserver
    ) {
        try {
            validate(request);

            UserActionAvro action =
                    UserActionMapper.toAvro(request);

            producer.send(action);

            responseObserver.onNext(
                    Empty.getDefaultInstance()
            );

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
                                    "Failed to collect user action"
                            )
                            .withCause(exception)
                            .asRuntimeException()
            );
        }
    }

    private void validate(UserActionProto request) {
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException(
                    "user_id must be positive"
            );
        }

        if (request.getEventId() <= 0) {
            throw new IllegalArgumentException(
                    "event_id must be positive"
            );
        }

        if (!request.hasTimestamp()) {
            throw new IllegalArgumentException(
                    "timestamp is required"
            );
        }

        if (request.getActionType()
                == ActionTypeProto.UNRECOGNIZED) {

            throw new IllegalArgumentException(
                    "action_type is not recognized"
            );
        }
    }
}