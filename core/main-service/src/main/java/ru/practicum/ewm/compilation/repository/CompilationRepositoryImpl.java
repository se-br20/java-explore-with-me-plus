package ru.practicum.ewm.compilation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.model.Event;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompilationRepositoryImpl implements CompilationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Compilation postCompilation(Compilation compilation) {
        String sql = "INSERT INTO compilations (title, pinned) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql, new String[]{"id"});
            preparedStatement.setString(1, compilation.getTitle());
            preparedStatement.setBoolean(2, Boolean.TRUE.equals(compilation.getPinned()));
            return preparedStatement;
        }, keyHolder);

        Long compId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        compilation.setId(compId);

        if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
            saveEventsRelations(compId, compilation.getEvents());
        }

        return compilation;
    }

    @Override
    public void deleteCompilation(Long compId) {
        jdbcTemplate.update(
                "DELETE FROM compilations WHERE id = ?",
                compId
        );
    }

    @Override
    public Compilation patchCompilation(Long compId, Compilation compilation, boolean replaceEvents) {
        if (compilation.getTitle() != null) {
            jdbcTemplate.update(
                    "UPDATE compilations SET title = ? WHERE id = ?",
                    compilation.getTitle(),
                    compId
            );
        }

        if (compilation.getPinned() != null) {
            jdbcTemplate.update(
                    "UPDATE compilations SET pinned = ? WHERE id = ?",
                    compilation.getPinned(),
                    compId
            );
        }

        if (replaceEvents) {
            jdbcTemplate.update(
                    "DELETE FROM compilation_events WHERE compilation_id = ?",
                    compId
            );

            if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
                saveEventsRelations(compId, compilation.getEvents());
            }
        }

        compilation.setId(compId);
        return compilation;
    }

    @Override
    public List<Compilation> getCompilations(Boolean pinned, int from, int size) {
        StringBuilder sql = new StringBuilder("SELECT id, title, pinned FROM compilations ");
        List<Object> params = new ArrayList<>();

        if (pinned != null) {
            sql.append("WHERE pinned = ? ");
            params.add(pinned);
        }

        sql.append("ORDER BY id LIMIT ? OFFSET ?");
        params.add(size);
        params.add(from);

        List<Compilation> compilations = jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> Compilation.builder()
                        .id(resultSet.getLong("id"))
                        .title(resultSet.getString("title"))
                        .pinned(resultSet.getBoolean("pinned"))
                        .events(new ArrayList<>())
                        .build(),
                params.toArray()
        );

        if (compilations.isEmpty()) {
            return Collections.emptyList();
        }

        fillEventsForCompilations(compilations);

        return compilations;
    }

    @Override
    public Optional<Compilation> getCompilationById(Long compId) {
        String compilationSql = "SELECT id, title, pinned FROM compilations WHERE id = ?";

        List<Compilation> compilations = jdbcTemplate.query(
                compilationSql,
                (resultSet, rowNumber) -> Compilation.builder()
                        .id(resultSet.getLong("id"))
                        .title(resultSet.getString("title"))
                        .pinned(resultSet.getBoolean("pinned"))
                        .events(new ArrayList<>())
                        .build(),
                compId
        );

        if (compilations.isEmpty()) {
            return Optional.empty();
        }

        Compilation compilation = compilations.getFirst();

        String eventsSql = """
                SELECT event_id
                FROM compilation_events
                WHERE compilation_id = ?
                ORDER BY event_id
                """;

        List<Long> eventIds = jdbcTemplate.queryForList(
                eventsSql,
                Long.class,
                compId
        );

        compilation.setEvents(
                eventIds.stream()
                        .map(id -> Event.builder().id(id).build())
                        .toList()
        );

        return Optional.of(compilation);
    }

    @Override
    public boolean existsById(Long compId) {
        String sql = "SELECT COUNT(*) FROM compilations WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, compId);
        return count != null && count > 0;
    }

    private void fillEventsForCompilations(List<Compilation> compilations) {
        List<Long> compIds = compilations.stream()
                .map(Compilation::getId)
                .toList();

        String eventsSql = """
                SELECT compilation_id, event_id
                FROM compilation_events
                WHERE compilation_id IN (:ids)
                ORDER BY event_id
                """;

        Map<Long, List<Event>> eventsByCompId = new HashMap<>();

        namedParameterJdbcTemplate.query(eventsSql, Map.of("ids", compIds), resultSet -> {
            Long compId = resultSet.getLong("compilation_id");
            Long eventId = resultSet.getLong("event_id");

            eventsByCompId.computeIfAbsent(compId, id -> new ArrayList<>())
                    .add(Event.builder().id(eventId).build());
        });

        for (Compilation compilation : compilations) {
            compilation.setEvents(
                    eventsByCompId.getOrDefault(compilation.getId(), Collections.emptyList())
            );
        }
    }

    private void saveEventsRelations(Long compId, List<Event> events) {
        String sql = """
                INSERT INTO compilation_events (compilation_id, event_id)
                VALUES (?, ?)
                """;

        List<Event> uniqueEvents = events.stream()
                .filter(Objects::nonNull)
                .filter(event -> event.getId() != null)
                .collect(
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toMap(
                                        Event::getId,
                                        event -> event,
                                        (first, second) -> first
                                ),
                                map -> new ArrayList<>(map.values())
                        )
                );

        jdbcTemplate.batchUpdate(sql, uniqueEvents, uniqueEvents.size(), (preparedStatement, event) -> {
            preparedStatement.setLong(1, compId);
            preparedStatement.setLong(2, event.getId());
        });
    }
}