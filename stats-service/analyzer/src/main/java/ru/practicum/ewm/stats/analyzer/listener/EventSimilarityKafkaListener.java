package ru.practicum.ewm.stats.analyzer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.service.EventSimilarityPersistenceService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityKafkaListener {

    private final EventSimilarityPersistenceService service;

    @KafkaListener(
            topics =
                    "${kafka.topics.events-similarity}",
            groupId =
                    "${kafka.groups.events-similarity}",
            containerFactory =
                    "eventSimilarityKafkaListenerContainerFactory"
    )
    public void handle(
            EventSimilarityAvro similarity
    ) {
        log.debug(
                "Analyzer received similarity: "
                        + "eventA={}, eventB={}, score={}",
                similarity.getEventA(),
                similarity.getEventB(),
                similarity.getScore()
        );

        service.process(similarity);
    }
}