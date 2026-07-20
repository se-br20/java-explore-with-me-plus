package ru.practicum.ewm.stats.analyzer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.service.UserActionPersistenceService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionKafkaListener {

    private final UserActionPersistenceService service;

    @KafkaListener(
            topics = "${kafka.topics.user-actions}",
            groupId = "${kafka.groups.user-actions}",
            containerFactory =
                    "userActionKafkaListenerContainerFactory"
    )
    public void handle(UserActionAvro action) {
        log.debug(
                "Analyzer received action: "
                        + "userId={}, eventId={}, type={}",
                action.getUserId(),
                action.getEventId(),
                action.getActionType()
        );

        service.process(action);
    }
}