package com.ecommerce.project.service;

import com.ecommerce.project.model.Category;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService{

    private List<Category> categories = new ArrayList<>();
    private long nextId = 0;
    @Override
    public List<Category> getAllCategories() {
        return categories;
    }

    @Override
    public void createCategory(Category category) {
        nextId++;
        Long id = nextId;
        category.setCategoryId(id);
        categories.add(category);
    }

    @Override
    public String deleteCategory(Long categoryId) {
        Category category = categories.stream()
                .filter(c -> c.getCategoryId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND , "Resource Not Found"));
        categories.remove(category);
        return "Category with categoryId : " + category.getCategoryId() + " deleted successfully";
    }

    @Override
    public String updateCategory(Category category) {
        Long findId = category.getCategoryId();
        String newCategoryName = category.getCategoryName();
        Category findCategory = categories.stream()
                .filter(c -> c.getCategoryId().equals(findId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        findCategory.setCategoryName(newCategoryName);
        return "Category with category Id : " + findId + " updated successfully";
    }
}
