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
@Table(name = "user_actions")
public class UserActionEntity {

    @EmbeddedId
    private UserActionId id;

    @Column(
            name = "rating",
            nullable = false
    )
    private Double rating;

    @Column(
            name = "last_interaction_at",
            nullable = false
    )
    private Instant lastInteractionAt;
}