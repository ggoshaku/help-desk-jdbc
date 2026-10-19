package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.Department;
import helpdesk.repository.DepartmentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcUtils.toLocalDateTime;

public class JdbcDepartmentRepository implements DepartmentRepository {

    private static final RowMapper<Department> MAPPER = rs -> new Department(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            toLocalDateTime(rs.getTimestamp("created_at"))
    );

    private final ConnectionProvider connections;

    public JdbcDepartmentRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<Department> findAll() {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM departments ORDER BY id");
             ResultSet rs = ps.executeQuery()) {

            List<Department> departments = new ArrayList<>();
            while (rs.next()) {
                departments.add(MAPPER.map(rs));
            }
            return departments;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения подразделений", e);
        }
    }

    @Override
    public Optional<Department> findById(long id) {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM departments WHERE id = ?")) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(MAPPER.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка поиска подразделения " + id, e);
        }
    }
}
