package com.ecommerce.project.service;

import com.ecommerce.project.model.Category;
import org.springframework.stereotype.Service;

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
                .findFirst().orElse(null);
        if(category == null){
            return "Category with categoryId : " + categoryId + " does not exist";
        }
        categories.remove(category);
        return "Category with categoryId : " + category.getCategoryId() + " deleted successfully";
    }
}
