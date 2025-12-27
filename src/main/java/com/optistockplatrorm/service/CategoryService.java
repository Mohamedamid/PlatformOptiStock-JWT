package com.optistockplatrorm.service;

import com.optistockplatrorm.dto.CategoryRequestDTO;
import com.optistockplatrorm.dto.CategoryResponseDTO;
import com.optistockplatrorm.entity.Category;
import com.optistockplatrorm.entity.Enums.Role;
import com.optistockplatrorm.mapper.CategoryMapper;
import com.optistockplatrorm.repository.CategoryRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryMapper categoryMapper;

    public Page<CategoryResponseDTO> getAllCategory(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        return categoryRepository.findAll(pageable).map(categoryMapper::toDto);
    }

    public CategoryResponseDTO getCategoryById(Long id) {

        Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Catégorie introuvable avec l'identifiant : " + id));
        return categoryMapper.toDto(category);
    }

    public CategoryResponseDTO createCategory(CategoryRequestDTO dto) {

        Category category = categoryMapper.toEntity(dto);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toDto(saved);
    }

    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO dto) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable avec l'identifiant : " + id));

        category.setName(dto.name());
        category.setDescription(dto.description());
        category.setActive(dto.active());

        Category updated = categoryRepository.save(category);
        return categoryMapper.toDto(updated);
    }

    public void deleteCategory(Long id) {

        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Catégorie introuvable avec l'identifiant : " + id);
        }

        categoryRepository.deleteById(id);
    }
}
