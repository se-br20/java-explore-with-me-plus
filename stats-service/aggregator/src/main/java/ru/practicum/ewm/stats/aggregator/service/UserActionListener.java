package ru.practicum.ewm.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener {

    private final SimilarityService similarityService;
    private final EventSimilarityProducer producer;

    @KafkaListener(
            topics = "${kafka.topics.user-actions}"
    )
    public void handle(UserActionAvro action) {
        log.debug(
                "Received user action: "
                        + "userId={}, eventId={}, action={}",
                action.getUserId(),
                action.getEventId(),
                action.getActionType()
        );

        List<EventSimilarityAvro> similarities =
                similarityService.update(action);

        similarities.forEach(producer::send);

        log.debug(
                "Produced {} similarity updates",
                similarities.size()
        );
    }
}