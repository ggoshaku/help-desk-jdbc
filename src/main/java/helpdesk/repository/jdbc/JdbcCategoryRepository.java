package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.Category;
import helpdesk.model.TicketPriority;
import helpdesk.repository.CategoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCategoryRepository implements CategoryRepository {

    private static final RowMapper<Category> MAPPER = rs -> new Category(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            TicketPriority.valueOf(rs.getString("default_priority")),
            rs.getBoolean("active")
    );

    private final ConnectionProvider connections;

    public JdbcCategoryRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<Category> findAll() {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM categories ORDER BY id");
             ResultSet rs = ps.executeQuery()) {

            List<Category> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(MAPPER.map(rs));
            }
            return categories;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения категорий", e);
        }
    }

    @Override
    public Optional<Category> findById(long id) {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM categories WHERE id = ?")) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(MAPPER.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка поиска категории " + id, e);
        }
    }

    @Override
    public boolean setActive(long id, boolean active) {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE categories SET active = ? WHERE id = ?")) {

            ps.setBoolean(1, active);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось изменить категорию " + id, e);
        }
    }
}
