package ru.practicum.ewm.stats.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_similarities")
public class EventSimilarityEntity {

    @EmbeddedId
    private EventSimilarityId id;

    @Column(
            name = "score",
            nullable = false
    )
    private Double score;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;
}