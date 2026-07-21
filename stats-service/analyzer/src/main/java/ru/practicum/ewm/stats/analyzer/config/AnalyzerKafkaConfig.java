package ru.practicum.ewm.stats.analyzer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.serialization.EventSimilarityAvroDeserializer;
import ru.practicum.ewm.stats.serialization.UserActionAvroDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class AnalyzerKafkaConfig {

    @Bean
    public ConsumerFactory<Long, UserActionAvro>
    userActionConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers
    ) {
        return new DefaultKafkaConsumerFactory<>(
                createBaseConsumerProperties(
                        bootstrapServers
                ),
                new LongDeserializer(),
                new UserActionAvroDeserializer()
        );
    }

    @Bean
    public ConsumerFactory<Long, EventSimilarityAvro>
    eventSimilarityConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers
    ) {
        return new DefaultKafkaConsumerFactory<>(
                createBaseConsumerProperties(
                        bootstrapServers
                ),
                new LongDeserializer(),
                new EventSimilarityAvroDeserializer()
        );
    }

    @Bean(
            name =
                    "userActionKafkaListenerContainerFactory"
    )
    public ConcurrentKafkaListenerContainerFactory<
            Long,
            UserActionAvro
            > userActionKafkaListenerContainerFactory(
            @Qualifier("userActionConsumerFactory")
            ConsumerFactory<Long, UserActionAvro>
                    consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<
                Long,
                UserActionAvro
                > factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.getContainerProperties()
                .setAckMode(
                        ContainerProperties.AckMode.RECORD
                );

        factory.setConcurrency(1);

        return factory;
    }

    @Bean(
            name =
                    "eventSimilarityKafkaListenerContainerFactory"
    )
    public ConcurrentKafkaListenerContainerFactory<
            Long,
            EventSimilarityAvro
            > eventSimilarityKafkaListenerContainerFactory(
            @Qualifier(
                    "eventSimilarityConsumerFactory"
            )
            ConsumerFactory<
                    Long,
                    EventSimilarityAvro
                    > consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<
                Long,
                EventSimilarityAvro
                > factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.getContainerProperties()
                .setAckMode(
                        ContainerProperties.AckMode.RECORD
                );

        factory.setConcurrency(1);

        return factory;
    }

    private Map<String, Object>
    createBaseConsumerProperties(
            String bootstrapServers
    ) {
        Map<String, Object> properties =
                new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        properties.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                100
        );

        return properties;
    }
}