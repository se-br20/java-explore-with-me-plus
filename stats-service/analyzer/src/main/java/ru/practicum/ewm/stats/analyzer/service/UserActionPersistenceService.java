package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.model.UserActionEntity;
import ru.practicum.ewm.stats.analyzer.model.UserActionId;
import ru.practicum.ewm.stats.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserActionPersistenceService {

    private final UserActionRepository repository;

    @Transactional
    public void process(UserActionAvro action) {
        validate(action);

        UserActionId id = new UserActionId(
                action.getUserId(),
                action.getEventId()
        );

        double receivedRating =
                resolveRating(action.getActionType());

        Instant receivedTimestamp =
                action.getTimestamp();

        Optional<UserActionEntity> existingOptional =
                repository.findById(id);

        if (existingOptional.isEmpty()) {
            UserActionEntity entity =
                    UserActionEntity.builder()
                            .id(id)
                            .rating(receivedRating)
                            .lastInteractionAt(
                                    receivedTimestamp
                            )
                            .build();

            repository.save(entity);
            return;
        }

        UserActionEntity existing =
                existingOptional.get();

        double newRating =
                Math.max(
                        existing.getRating(),
                        receivedRating
                );

        Instant newTimestamp =
                latest(
                        existing.getLastInteractionAt(),
                        receivedTimestamp
                );

        boolean ratingChanged =
                Double.compare(
                        existing.getRating(),
                        newRating
                ) != 0;

        boolean timestampChanged =
                !existing.getLastInteractionAt()
                        .equals(newTimestamp);

        if (!ratingChanged && !timestampChanged) {
            return;
        }

        existing.setRating(newRating);
        existing.setLastInteractionAt(newTimestamp);

        repository.save(existing);
    }

    private double resolveRating(
            ActionTypeAvro actionType
    ) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    private Instant latest(
            Instant first,
            Instant second
    ) {
        return first.isAfter(second)
                ? first
                : second;
    }

    private void validate(UserActionAvro action) {
        if (action == null) {
            throw new IllegalArgumentException(
                    "User action must not be null"
            );
        }

        if (action.getUserId() <= 0) {
            throw new IllegalArgumentException(
                    "userId must be positive"
            );
        }

        if (action.getEventId() <= 0) {
            throw new IllegalArgumentException(
                    "eventId must be positive"
            );
        }

        if (action.getActionType() == null) {
            throw new IllegalArgumentException(
                    "actionType must not be null"
            );
        }

        if (action.getTimestamp() == null) {
            throw new IllegalArgumentException(
                    "timestamp must not be null"
            );
        }
    }
}
