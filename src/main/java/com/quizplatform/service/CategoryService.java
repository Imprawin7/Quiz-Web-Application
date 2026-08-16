package com.quizplatform.service;

import com.quizplatform.model.Category;
import com.quizplatform.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public static class CategoryException extends RuntimeException {
        public CategoryException(String message) { super(message); }
    }

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    }

    public Category create(String name, String description) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new CategoryException("A category named \"" + name + "\" already exists.");
        }
        return categoryRepository.save(new Category(name.trim(), description));
    }

    public Category update(Long id, String name, String description) {
        Category category = getById(id);
        categoryRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new CategoryException("A category named \"" + name + "\" already exists.");
            }
        });
        category.setName(name.trim());
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}