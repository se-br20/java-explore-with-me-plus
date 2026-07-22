package ru.practicum.ewm.event.dto;

public interface EventCountsAware extends Commentable, Rateable {

    Long getConfirmedRequests();

    void setConfirmedRequests(Long confirmedRequests);
}