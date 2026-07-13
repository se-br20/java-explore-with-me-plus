package ru.practicum.ewm.compilation.dto;

import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;

import java.util.ArrayList;
import java.util.List;

public final class CompilationMapper {

    private CompilationMapper() {
    }

    public static Compilation toCompilation(NewCompilationDto dto) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(Boolean.TRUE.equals(dto.getPinned()))
                .events(toEventRefs(dto.getEvents()))
                .build();
    }

    public static CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(events == null ? List.of() : events)
                .pinned(Boolean.TRUE.equals(compilation.getPinned()))
                .title(compilation.getTitle())
                .build();
    }

    private static List<Event> toEventRefs(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }

        return eventIds.stream()
                .distinct()
                .map(id -> Event.builder().id(id).build())
                .toList();
    }
}