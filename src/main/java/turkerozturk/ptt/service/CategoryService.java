package turkerozturk.ptt.service;

import turkerozturk.ptt.entity.Category;
import turkerozturk.ptt.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;



    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        Optional<Category> optionalCategory = categoryRepository.findById(id);
        if (optionalCategory.isPresent()) {
            Category category = optionalCategory.get();
            if (category.getTopics() != null && !category.getTopics().isEmpty()) {
                // Bağlı topic'ler varsa silme!
                throw new IllegalStateException("Cannot delete category with associated topics.");
            }
            categoryRepository.delete(category);
        }
    }

}