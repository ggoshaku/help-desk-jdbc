package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.TicketStatus;
import helpdesk.model.TicketStatusHistory;
import helpdesk.repository.StatusHistoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static helpdesk.repository.jdbc.JdbcUtils.getNullableLong;
import static helpdesk.repository.jdbc.JdbcUtils.toLocalDateTime;

public class JdbcStatusHistoryRepository implements StatusHistoryRepository {

    private final ConnectionProvider connections;

    public JdbcStatusHistoryRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<TicketStatusHistory> findByTicketId(long ticketId) {
        String sql = """
                SELECT h.id, h.ticket_id, h.old_status, h.new_status,
                       h.changed_by_id, u.name AS changed_by_name, h.changed_at
                FROM ticket_status_history h
                LEFT JOIN users u ON u.id = h.changed_by_id
                WHERE h.ticket_id = ?
                ORDER BY h.changed_at, h.id
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, ticketId);
            List<TicketStatusHistory> history = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String oldStatus = rs.getString("old_status");
                    history.add(new TicketStatusHistory(
                            rs.getLong("id"),
                            rs.getLong("ticket_id"),
                            oldStatus == null ? null : TicketStatus.valueOf(oldStatus),
                            TicketStatus.valueOf(rs.getString("new_status")),
                            getNullableLong(rs, "changed_by_id"),
                            rs.getString("changed_by_name"),
                            toLocalDateTime(rs.getTimestamp("changed_at"))
                    ));
                }
            }
            return history;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения истории заявки " + ticketId, e);
        }
    }
}
