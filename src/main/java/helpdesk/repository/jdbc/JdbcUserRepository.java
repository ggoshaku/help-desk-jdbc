package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.Administrator;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.User;
import helpdesk.model.UserRole;
import helpdesk.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcUtils.getNullableLong;
import static helpdesk.repository.jdbc.JdbcUtils.setNullableLong;
import static helpdesk.repository.jdbc.JdbcUtils.toLocalDateTime;

public class JdbcUserRepository implements UserRepository {

    private final ConnectionProvider connections;

    public JdbcUserRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public User save(User user) {
        String sql = """
                INSERT INTO users(name, email, role, department_id, active)
                VALUES (?, ?, ?::user_role, ?, ?)
                RETURNING id, created_at
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole().name());
            setNullableLong(ps, 4, user.getDepartmentId());
            ps.setBoolean(5, user.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                user.markSaved(rs.getLong("id"), toLocalDateTime(rs.getTimestamp("created_at")));
            }
            return user;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось сохранить пользователя " + user.getEmail(), e);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        return findOne("SELECT * FROM users WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findOne("SELECT * FROM users WHERE email = ?", ps -> ps.setString(1, email.strip().toLowerCase()));
    }

    @Override
    public List<User> findAll() {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {

            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения пользователей", e);
        }
    }

    private Optional<User> findOne(String sql, StatementBinder binder) {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка поиска пользователя", e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        Long departmentId = getNullableLong(rs, "department_id");
        boolean active = rs.getBoolean("active");
        LocalDateTime createdAt = toLocalDateTime(rs.getTimestamp("created_at"));

        return switch (UserRole.valueOf(rs.getString("role"))) {
            case CUSTOMER -> new Customer(id, name, email, departmentId, active, createdAt);
            case SUPPORT_AGENT -> new SupportAgent(id, name, email, departmentId, active, createdAt);
            case ADMIN -> new Administrator(id, name, email, departmentId, active, createdAt);
        };
    }
}
