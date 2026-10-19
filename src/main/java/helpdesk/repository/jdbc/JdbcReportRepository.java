package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.TicketDetails;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.repository.ReportRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcUtils.toLocalDateTime;

public class JdbcReportRepository implements ReportRepository {

    private static final String SELECT_DETAILS = """
            SELECT
                t.id,
                t.title,
                t.status,
                t.priority,
                c.name AS category_name,
                customer.name AS customer_name,
                assignee.name AS assignee_name,
                t.created_at,
                t.updated_at,
                t.closed_at,
                t.due_at
            FROM tickets t
            JOIN users customer ON customer.id = t.customer_id
            LEFT JOIN users assignee ON assignee.id = t.assignee_id
            LEFT JOIN categories c ON c.id = t.category_id
            """;

    private static final RowMapper<TicketDetails> DETAILS_MAPPER = rs -> new TicketDetails(
            rs.getLong("id"),
            rs.getString("title"),
            TicketStatus.valueOf(rs.getString("status")),
            TicketPriority.valueOf(rs.getString("priority")),
            rs.getString("category_name"),
            rs.getString("customer_name"),
            rs.getString("assignee_name"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")),
            toLocalDateTime(rs.getTimestamp("closed_at")),
            toLocalDateTime(rs.getTimestamp("due_at"))
    );

    private final ConnectionProvider connections;

    public JdbcReportRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Optional<TicketDetails> findDetails(long ticketId) {
        return query(SELECT_DETAILS + " WHERE t.id = ?", ps -> ps.setLong(1, ticketId), DETAILS_MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public List<TicketDetails> findAllDetails() {
        return query(SELECT_DETAILS + " ORDER BY t.created_at DESC", ps -> {
        }, DETAILS_MAPPER);
    }

    @Override
    public Map<TicketStatus, Long> countByStatus() {
        String sql = """
                SELECT status, COUNT(*) AS count
                FROM tickets
                GROUP BY status
                ORDER BY status
                """;

        Map<TicketStatus, Long> result = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            result.put(status, 0L);
        }
        query(sql, ps -> {
        }, rs -> result.put(TicketStatus.valueOf(rs.getString("status")), rs.getLong("count")));
        return result;
    }

    @Override
    public Map<String, Long> countByCategory() {
        String sql = """
                SELECT c.name, COUNT(t.id) AS ticket_count
                FROM categories c
                LEFT JOIN tickets t ON t.category_id = c.id
                GROUP BY c.id, c.name
                ORDER BY ticket_count DESC, c.name
                """;
        return toMap(sql, "name", "ticket_count");
    }

    @Override
    public Map<String, Long> agentWorkload() {
        String sql = """
                SELECT u.name,
                       COUNT(t.id) FILTER (
                           WHERE t.status IN ('NEW', 'IN_PROGRESS')
                       ) AS active_tickets
                FROM users u
                LEFT JOIN tickets t ON t.assignee_id = u.id
                WHERE u.role = 'SUPPORT_AGENT'
                GROUP BY u.id, u.name
                ORDER BY active_tickets DESC, u.name
                """;
        return toMap(sql, "name", "active_tickets");
    }

    private Map<String, Long> toMap(String sql, String keyColumn, String valueColumn) {
        Map<String, Long> result = new LinkedHashMap<>();
        query(sql, ps -> {
        }, rs -> result.put(rs.getString(keyColumn), rs.getLong(valueColumn)));
        return result;
    }

    private <T> List<T> query(String sql, StatementBinder binder, RowMapper<T> mapper) {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            binder.bind(ps);
            List<T> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка выполнения отчёта", e);
        }
    }
}
