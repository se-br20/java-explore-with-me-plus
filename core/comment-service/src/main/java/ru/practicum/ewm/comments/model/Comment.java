package ru.practicum.ewm.comments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(
            name = "author_name",
            nullable = false,
            length = 250
    )
    private String authorName;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private LocalDateTime created;

    private LocalDateTime updated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentStatus status;

    @Column(name = "moderator_id")
    private Long moderatorId;

    @Column(name = "moderator_name", length = 250)
    private String moderatorName;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;
}