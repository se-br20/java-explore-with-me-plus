package ru.practicum.ewm.stats.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserActionProducer {

    private static final long SEND_TIMEOUT_SECONDS = 5L;

    private final KafkaTemplate<Long, UserActionAvro>
            kafkaTemplate;

    @Value("${kafka.topics.user-actions}")
    private String userActionsTopic;

    public void send(UserActionAvro action) {
        try {
            kafkaTemplate.send(
                            userActionsTopic,

                            action.getUserId(),

                            action
                    )
                    .get(
                            SEND_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Sending user action was interrupted",
                    exception
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to send user action to Kafka",
                    exception
            );
        }
    }
}