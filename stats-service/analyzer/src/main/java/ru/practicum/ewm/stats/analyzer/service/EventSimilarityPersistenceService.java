package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityId;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventSimilarityPersistenceService {

    private static final double MIN_SCORE = 0.0;
    private static final double MAX_SCORE = 1.0;

    private final EventSimilarityRepository repository;

    @Transactional
    public void process(
            EventSimilarityAvro similarity
    ) {
        validate(similarity);

        long eventA = Math.min(
                similarity.getEventA(),
                similarity.getEventB()
        );

        long eventB = Math.max(
                similarity.getEventA(),
                similarity.getEventB()
        );

        EventSimilarityId id =
                new EventSimilarityId(
                        eventA,
                        eventB
                );

        Instant receivedTimestamp =
                similarity.getTimestamp();

        Optional<EventSimilarityEntity>
                existingOptional =
                repository.findById(id);

        if (existingOptional.isEmpty()) {
            EventSimilarityEntity entity =
                    EventSimilarityEntity.builder()
                            .id(id)
                            .score(
                                    similarity.getScore()
                            )
                            .updatedAt(
                                    receivedTimestamp
                            )
                            .build();

            repository.save(entity);
            return;
        }

        EventSimilarityEntity existing =
                existingOptional.get();

        if (existing.getUpdatedAt()
                .isAfter(receivedTimestamp)) {
            return;
        }

        existing.setScore(similarity.getScore());
        existing.setUpdatedAt(receivedTimestamp);

        repository.save(existing);
    }

    private void validate(
            EventSimilarityAvro similarity
    ) {
        if (similarity == null) {
            throw new IllegalArgumentException(
                    "Similarity must not be null"
            );
        }

        if (similarity.getEventA() <= 0
                || similarity.getEventB() <= 0) {

            throw new IllegalArgumentException(
                    "Event ids must be positive"
            );
        }

        if (similarity.getEventA()
                == similarity.getEventB()) {

            throw new IllegalArgumentException(
                    "An event cannot be similar to itself"
            );
        }

        double score = similarity.getScore();

        if (!Double.isFinite(score)
                || score < MIN_SCORE
                || score > MAX_SCORE) {

            throw new IllegalArgumentException(
                    "Similarity score must be "
                            + "between 0.0 and 1.0"
            );
        }

        if (similarity.getTimestamp() == null) {
            throw new IllegalArgumentException(
                    "timestamp must not be null"
            );
        }
    }
}