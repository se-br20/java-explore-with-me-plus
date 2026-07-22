package ru.practicum.stat.client;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CollectorClient {

    private static final long DEADLINE_SECONDS = 5L;

    @GrpcClient("collector")
    private UserActionControllerGrpc
            .UserActionControllerBlockingStub collectorStub;

    public void sendAction(
            long userId,
            long eventId,
            UserActionType actionType
    ) {
        sendAction(
                userId,
                eventId,
                actionType,
                Instant.now()
        );
    }

    public void sendAction(
            long userId,
            long eventId,
            UserActionType actionType,
            Instant timestamp
    ) {
        validate(
                userId,
                eventId,
                actionType,
                timestamp
        );

        UserActionProto request =
                UserActionProto.newBuilder()
                        .setUserId(userId)
                        .setEventId(eventId)
                        .setActionType(
                                toProto(actionType)
                        )
                        .setTimestamp(
                                toProto(timestamp)
                        )
                        .build();

        try {
            collectorStub
                    .withWaitForReady()
                    .withDeadlineAfter(
                            DEADLINE_SECONDS,
                            TimeUnit.SECONDS
                    )
                    .collectUserAction(request);

            log.debug(
                    "Action sent to Collector: "
                            + "userId={}, eventId={}, type={}",
                    userId,
                    eventId,
                    actionType
            );

        } catch (StatusRuntimeException exception) {

            log.warn(
                    "Collector request failed: "
                            + "userId={}, eventId={}, "
                            + "type={}, status={}",
                    userId,
                    eventId,
                    actionType,
                    exception.getStatus()
            );
        }
    }

    private ActionTypeProto toProto(
            UserActionType actionType
    ) {
        ActionTypeProto protoType =
                ActionTypeProto.forNumber(
                        actionType.getProtoNumber()
                );

        if (protoType == null) {
            throw new IllegalArgumentException(
                    "Unsupported action type: "
                            + actionType
            );
        }

        return protoType;
    }

    private Timestamp toProto(
            Instant timestamp
    ) {
        return Timestamp.newBuilder()
                .setSeconds(
                        timestamp.getEpochSecond()
                )
                .setNanos(
                        timestamp.getNano()
                )
                .build();
    }

    private void validate(
            long userId,
            long eventId,
            UserActionType actionType,
            Instant timestamp
    ) {
        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "userId must be positive"
            );
        }

        if (eventId <= 0) {
            throw new IllegalArgumentException(
                    "eventId must be positive"
            );
        }

        Objects.requireNonNull(
                actionType,
                "actionType must not be null"
        );

        Objects.requireNonNull(
                timestamp,
                "timestamp must not be null"
        );
    }
}