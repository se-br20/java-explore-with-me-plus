package ru.practicum.ewm.categories.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl
        implements CategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Category postCategory(
            Category category
    ) {
        String sql =
                "INSERT INTO categories (name) "
                        + "VALUES (?)";

        KeyHolder keyHolder =
                new GeneratedKeyHolder();

        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    sql,
                                    new String[]{"id"}
                            );

                    statement.setString(
                            1,
                            category.getName()
                    );

                    return statement;
                },
                keyHolder
        );

        category.setId(
                Objects.requireNonNull(
                        keyHolder.getKey()
                ).longValue()
        );

        return category;
    }

    @Override
    public void deleteCategory(
            Long catId
    ) {
        String sql =
                "DELETE FROM categories "
                        + "WHERE id = ?";

        jdbcTemplate.update(
                sql,
                catId
        );
    }

    @Override
    public Category patchCategory(
            Long catId,
            Category category
    ) {
        String sql =
                "UPDATE categories "
                        + "SET name = ? "
                        + "WHERE id = ?";

        int updated =
                jdbcTemplate.update(
                        sql,
                        category.getName(),
                        catId
                );

        if (updated == 0) {
            throw new NotFoundException(
                    "Category with id="
                            + catId
                            + " was not found"
            );
        }

        return new Category(
                catId,
                category.getName()
        );
    }

    @Override
    public List<Category> getCategories(
            int from,
            int size
    ) {
        String sql =
                "SELECT * "
                        + "FROM categories "
                        + "ORDER BY id "
                        + "LIMIT ? OFFSET ?";

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) ->
                        new Category(
                                resultSet.getLong("id"),
                                resultSet.getString("name")
                        ),
                size,
                from
        );
    }

    @Override
    public Category getCategory(
            Long catId
    ) {
        String sql =
                "SELECT * "
                        + "FROM categories "
                        + "WHERE id = ?";

        try {
            return jdbcTemplate.queryForObject(
                    sql,
                    (resultSet, rowNumber) ->
                            new Category(
                                    resultSet.getLong("id"),
                                    resultSet.getString(
                                            "name"
                                    )
                            ),
                    catId
            );

        } catch (EmptyResultDataAccessException exception) {
            throw new NotFoundException(
                    "Category with id="
                            + catId
                            + " was not found"
            );
        }
    }

    @Override
    public boolean existsById(
            Long catId
    ) {
        String sql =
                "SELECT COUNT(*) "
                        + "FROM categories "
                        + "WHERE id = ?";

        Integer count =
                jdbcTemplate.queryForObject(
                        sql,
                        Integer.class,
                        catId
                );

        return count != null
                && count > 0;
    }
}