package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.AssignmentChange;
import helpdesk.repository.AssignmentHistoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static helpdesk.repository.jdbc.JdbcUtils.setNullableLong;
import static helpdesk.repository.jdbc.JdbcUtils.toLocalDateTime;

public class JdbcAssignmentHistoryRepository implements AssignmentHistoryRepository {

    private final ConnectionProvider connections;

    public JdbcAssignmentHistoryRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public void add(long ticketId, Long oldAssigneeId, long newAssigneeId, long changedById) {
        String sql = """
                INSERT INTO ticket_assignment_history(
                    ticket_id, old_assignee_id, new_assignee_id, changed_by_id
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, ticketId);
            setNullableLong(ps, 2, oldAssigneeId);
            ps.setLong(3, newAssigneeId);
            ps.setLong(4, changedById);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось записать историю назначения заявки " + ticketId, e);
        }
    }

    @Override
    public List<AssignmentChange> findByTicketId(long ticketId) {
        String sql = """
                SELECT h.ticket_id,
                       old_assignee.name AS old_assignee_name,
                       new_assignee.name AS new_assignee_name,
                       changed_by.name AS changed_by_name,
                       h.changed_at
                FROM ticket_assignment_history h
                LEFT JOIN users old_assignee ON old_assignee.id = h.old_assignee_id
                JOIN users new_assignee ON new_assignee.id = h.new_assignee_id
                JOIN users changed_by ON changed_by.id = h.changed_by_id
                WHERE h.ticket_id = ?
                ORDER BY h.changed_at, h.id
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, ticketId);
            List<AssignmentChange> history = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new AssignmentChange(
                            rs.getLong("ticket_id"),
                            rs.getString("old_assignee_name"),
                            rs.getString("new_assignee_name"),
                            rs.getString("changed_by_name"),
                            toLocalDateTime(rs.getTimestamp("changed_at"))
                    ));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения истории назначений заявки " + ticketId, e);
        }
    }
}
