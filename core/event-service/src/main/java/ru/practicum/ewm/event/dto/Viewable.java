package ru.practicum.ewm.event.dto;

import java.time.LocalDateTime;

public interface Viewable {

    Long getId();

    LocalDateTime getPublishedOn();

    void setViews(Long views);
}