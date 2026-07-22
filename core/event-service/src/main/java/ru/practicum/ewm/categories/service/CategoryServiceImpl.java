package ru.practicum.ewm.categories.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.categories.dto.CategoryDto;
import ru.practicum.ewm.categories.dto.CategoryMapper;
import ru.practicum.ewm.categories.dto.NewCategoryDto;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.categories.repository.CategoryRepository;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.exceptions.ConditionsNotMetException;
import ru.practicum.ewm.exceptions.exceptions.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    public CategoryDto postCategory(
            NewCategoryDto newCategoryDto
    ) {
        Category category =
                CategoryMapper.toCategory(
                        newCategoryDto
                );

        Category savedCategory =
                categoryRepository.save(category);

        return CategoryMapper.toCategoryDto(
                savedCategory
        );
    }

    @Override
    public void deleteCategory(Long catId) {
        Category category =
                getCategoryOrThrow(catId);

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConditionsNotMetException(
                    "Category with id="
                            + catId
                            + " is not empty"
            );
        }

        categoryRepository.delete(category);
    }

    @Override
    public CategoryDto patchCategory(
            Long catId,
            NewCategoryDto newCategoryDto
    ) {
        Category category =
                getCategoryOrThrow(catId);

        category.setName(
                newCategoryDto.getName()
        );

        Category updatedCategory =
                categoryRepository.save(category);

        return CategoryMapper.toCategoryDto(
                updatedCategory
        );
    }

    @Override
    public List<CategoryDto> getCategories(
            int from,
            int size
    ) {
        PageRequest pageRequest =
                PageRequest.of(
                        from / size,
                        size,
                        Sort.by(
                                Sort.Direction.ASC,
                                "id"
                        )
                );

        return categoryRepository
                .findAll(pageRequest)
                .stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        return CategoryMapper.toCategoryDto(
                getCategoryOrThrow(catId)
        );
    }

    private Category getCategoryOrThrow(
            Long catId
    ) {
        return categoryRepository
                .findById(catId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Category with id="
                                        + catId
                                        + " was not found"
                        )
                );
    }
}