package ru.practicum.ewm.event.dto.paramDto;


import java.time.LocalDateTime;

public interface DateRangeValidatable {
    LocalDateTime getRangeStart();

    LocalDateTime getRangeEnd();
}
