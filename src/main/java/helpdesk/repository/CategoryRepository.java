package helpdesk.repository;

import helpdesk.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    List<Category> findAll();

    Optional<Category> findById(long id);
}
