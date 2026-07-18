package ru.practicum.ewm.event.dto;


import java.time.LocalDateTime;

public interface Viewable {
    Long getId();

    void setViews(Long views);

    LocalDateTime getPublishedOn();
}