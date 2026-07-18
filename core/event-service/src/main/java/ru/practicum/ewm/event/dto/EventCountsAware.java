package ru.practicum.ewm.event.dto;

public interface EventCountsAware extends Commentable, Viewable {

    Long getConfirmedRequests();

    void setConfirmedRequests(Long confirmedRequests);
}