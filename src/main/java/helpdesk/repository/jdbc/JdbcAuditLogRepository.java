package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.AuditAction;
import helpdesk.model.AuditEntry;
import helpdesk.repository.AuditLogRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static helpdesk.repository.jdbc.JdbcUtils.getNullableLong;
import static helpdesk.repository.jdbc.JdbcUtils.toLocalDateTime;

public class JdbcAuditLogRepository implements AuditLogRepository {

    private final ConnectionProvider connections;

    public JdbcAuditLogRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public void add(long userId, AuditAction action, String entityType, long entityId, String details) {
        String sql = """
                INSERT INTO audit_log(user_id, action, entity_type, entity_id, details)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, action.name());
            ps.setString(3, entityType);
            ps.setLong(4, entityId);
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось записать действие " + action + " в журнал", e);
        }
    }

    @Override
    public List<AuditEntry> findByEntity(String entityType, long entityId) {
        String sql = """
                SELECT a.*, u.name AS user_name
                FROM audit_log a
                LEFT JOIN users u ON u.id = a.user_id
                WHERE a.entity_type = ? AND a.entity_id = ?
                ORDER BY a.created_at, a.id
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, entityType);
            ps.setLong(2, entityId);
            List<AuditEntry> entries = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new AuditEntry(
                            rs.getLong("id"),
                            getNullableLong(rs, "user_id"),
                            rs.getString("user_name"),
                            AuditAction.valueOf(rs.getString("action")),
                            rs.getString("entity_type"),
                            rs.getLong("entity_id"),
                            rs.getString("details"),
                            toLocalDateTime(rs.getTimestamp("created_at"))
                    ));
                }
            }
            return entries;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения журнала аудита", e);
        }
    }
}
