package ru.practicum.ewm.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EventSimilarityProducer {

    private static final long SEND_TIMEOUT_SECONDS = 5L;

    private final KafkaTemplate<Long, EventSimilarityAvro>
            kafkaTemplate;

    @Value("${kafka.topics.events-similarity}")
    private String similaritiesTopic;

    public void send(
            EventSimilarityAvro similarity
    ) {
        try {
            kafkaTemplate.send(
                            similaritiesTopic,

                            similarity.getEventA(),

                            similarity
                    )
                    .get(
                            SEND_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Sending event similarity was interrupted",
                    exception
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to send event similarity to Kafka",
                    exception
            );
        }
    }
}
