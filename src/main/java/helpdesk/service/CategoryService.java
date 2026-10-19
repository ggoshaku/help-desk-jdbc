package helpdesk.service;

import helpdesk.exception.CategoryNotFoundException;
import helpdesk.model.Category;
import helpdesk.repository.CategoryRepository;

import java.util.List;

public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public void deactivateCategory(long id) {
        if (!categoryRepository.setActive(id, false)) {
            throw new CategoryNotFoundException(id);
        }
    }

    public void activateCategory(long id) {
        if (!categoryRepository.setActive(id, true)) {
            throw new CategoryNotFoundException(id);
        }
    }
}
