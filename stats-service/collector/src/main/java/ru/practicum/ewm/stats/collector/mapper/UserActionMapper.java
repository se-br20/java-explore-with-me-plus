package ru.practicum.ewm.stats.collector.mapper;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

import java.time.Instant;

public final class UserActionMapper {

    private UserActionMapper() {
    }

    public static UserActionAvro toAvro(
            UserActionProto proto
    ) {
        Instant timestamp =
                Instant.ofEpochSecond(
                        proto.getTimestamp().getSeconds(),
                        proto.getTimestamp().getNanos()
                );

        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(
                        toAvroActionType(
                                proto.getActionType()
                        )
                )
                .setTimestamp(timestamp)
                .build();
    }

    private static ActionTypeAvro toAvroActionType(
            ActionTypeProto actionType
    ) {
        return switch (actionType) {
            case ACTION_VIEW ->
                    ActionTypeAvro.VIEW;

            case ACTION_REGISTER ->
                    ActionTypeAvro.REGISTER;

            case ACTION_LIKE ->
                    ActionTypeAvro.LIKE;

            case UNRECOGNIZED ->
                    throw new IllegalArgumentException(
                            "Unsupported action type"
                    );
        };
    }
}