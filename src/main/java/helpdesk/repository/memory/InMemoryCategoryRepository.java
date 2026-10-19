package helpdesk.repository.memory;

import helpdesk.model.Category;
import helpdesk.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCategoryRepository implements CategoryRepository {
    private final Map<Long, Category> categories = new LinkedHashMap<>();

    public void add(Category category) {
        categories.put(category.getId(), category);
    }

    @Override
    public List<Category> findAll() {
        return new ArrayList<>(categories.values());
    }

    @Override
    public Optional<Category> findById(long id) {
        return Optional.ofNullable(categories.get(id));
    }

    @Override
    public boolean setActive(long id, boolean active) {
        Category old = categories.get(id);
        if (old == null) {
            return false;
        }
        categories.put(id, new Category(id, old.getName(), old.getDescription(), old.getDefaultPriority(), active));
        return true;
    }
}
