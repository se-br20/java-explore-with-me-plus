package ru.practicum.ewm.stats.aggregator.model;

public record EventPair(
        long eventA,
        long eventB
) {

    public EventPair {
        if (eventA >= eventB) {
            throw new IllegalArgumentException(
                    "eventA must be less than eventB"
            );
        }
    }

    public static EventPair of(
            long firstEventId,
            long secondEventId
    ) {
        if (firstEventId == secondEventId) {
            throw new IllegalArgumentException(
                    "An event cannot be compared with itself"
            );
        }

        if (firstEventId < secondEventId) {
            return new EventPair(
                    firstEventId,
                    secondEventId
            );
        }

        return new EventPair(
                secondEventId,
                firstEventId
        );
    }
}