package helpdesk.repository;

import helpdesk.model.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {

    List<Department> findAll();

    Optional<Department> findById(long id);
}
