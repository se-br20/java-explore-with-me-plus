package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.CompilationMapper;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventService eventService;

    @Override
    @Transactional
    public CompilationDto postCompilation(NewCompilationDto newCompilationDto) {
        validateEventsExist(newCompilationDto.getEvents());

        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto);
        Compilation saved = compilationRepository.postCompilation(compilation);

        List<EventShortDto> eventDtos = getEventShortDtos(
                saved.getEvents().stream()
                        .map(Event::getId)
                        .toList()
        );

        return CompilationMapper.toCompilationDto(saved, eventDtos);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }

        compilationRepository.deleteCompilation(compId);
    }

    @Override
    @Transactional
    public CompilationDto patchCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.getCompilationById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        boolean replaceEvents = updateRequest.getEvents() != null;

        if (replaceEvents) {
            validateEventsExist(updateRequest.getEvents());
        }

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (replaceEvents) {
            List<Event> newEvents = updateRequest.getEvents().stream()
                    .distinct()
                    .map(id -> Event.builder().id(id).build())
                    .toList();

            compilation.setEvents(newEvents);
        }

        compilationRepository.patchCompilation(compId, compilation, replaceEvents);

        Compilation updated = compilationRepository.getCompilationById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        List<EventShortDto> eventDtos = getEventShortDtos(
                updated.getEvents().stream()
                        .map(Event::getId)
                        .toList()
        );

        return CompilationMapper.toCompilationDto(updated, eventDtos);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        List<Compilation> compilations = compilationRepository.getCompilations(pinned, from, size);

        if (compilations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> allEventIds = compilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream())
                .map(Event::getId)
                .collect(Collectors.toSet());

        Map<Long, EventShortDto> eventsMap = eventService.getShortDtosByIds(allEventIds)
                .stream()
                .collect(Collectors.toMap(EventShortDto::getId, event -> event));

        return compilations.stream()
                .map(compilation -> {
                    List<EventShortDto> events = compilation.getEvents().stream()
                            .map(Event::getId)
                            .map(eventsMap::get)
                            .filter(Objects::nonNull)
                            .toList();

                    return CompilationMapper.toCompilationDto(compilation, events);
                })
                .toList();
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.getCompilationById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        List<EventShortDto> events = getEventShortDtos(
                compilation.getEvents().stream()
                        .map(Event::getId)
                        .toList()
        );

        return CompilationMapper.toCompilationDto(compilation, events);
    }

    private List<EventShortDto> getEventShortDtos(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        return eventService.getShortDtosByIds(eventIds);
    }

    private void validateEventsExist(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new HashSet<>(eventIds);
        List<EventShortDto> foundEvents = eventService.getShortDtosByIds(uniqueIds);

        if (foundEvents.size() != uniqueIds.size()) {
            throw new NotFoundException("One or more events were not found");
        }
    }
}