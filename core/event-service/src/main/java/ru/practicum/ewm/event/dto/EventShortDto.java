package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.categories.dto.CategoryDto;
import ru.practicum.interaction.user.UserShortDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto
        implements EventCountsAware,
        Rateable,
        Viewable {

    private Long id;

    private String annotation;

    private CategoryDto category;

    private Long confirmedRequests;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime eventDate;

    private UserShortDto initiator;

    private Boolean paid;

    @JsonIgnore
    private LocalDateTime publishedOn;

    private String title;

    @Builder.Default
    private Double rating = 0.0;

    @Builder.Default
    private Long commentsCount = 0L;

    @Builder.Default
    private Long views = 0L;
}