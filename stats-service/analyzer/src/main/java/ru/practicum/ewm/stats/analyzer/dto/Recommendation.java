package ru.practicum.ewm.stats.analyzer.dto;

public record Recommendation(
        long eventId,
        double score
) {
}