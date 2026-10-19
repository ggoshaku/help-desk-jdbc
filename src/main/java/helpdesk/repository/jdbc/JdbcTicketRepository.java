package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.repository.TicketRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcUtils.getNullableLong;
import static helpdesk.repository.jdbc.JdbcUtils.setNullableDateTime;
import static helpdesk.repository.jdbc.JdbcUtils.setNullableLong;
import static helpdesk.repository.jdbc.JdbcUtils.toLocalDateTime;

public class JdbcTicketRepository implements TicketRepository {

    private final ConnectionProvider connections;

    public JdbcTicketRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Ticket save(Ticket ticket) {
        String sql = """
                INSERT INTO tickets(
                    title, description, status, priority,
                    customer_id, assignee_id, category_id, due_at
                )
                VALUES (?, ?, ?::ticket_status, ?::ticket_priority, ?, ?, ?, ?)
                RETURNING id, created_at, updated_at
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getStatus().name());
            ps.setString(4, ticket.getPriority().name());
            ps.setLong(5, ticket.getCustomerId());
            setNullableLong(ps, 6, ticket.getAssigneeId());
            setNullableLong(ps, 7, ticket.getCategoryId());
            setNullableDateTime(ps, 8, ticket.getDueAt());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                ticket.markSaved(
                        rs.getLong("id"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))
                );
            }
            return ticket;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось сохранить заявку", e);
        }
    }

    @Override
    public Optional<Ticket> findById(long id) {
        return findOne("SELECT * FROM tickets WHERE id = ?", id);
    }

    @Override
    public Optional<Ticket> findByIdForUpdate(long id) {
        return findOne("SELECT * FROM tickets WHERE id = ? FOR UPDATE", id);
    }

    private Optional<Ticket> findOne(String sql, long id) {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapTicket(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка поиска заявки " + id, e);
        }
    }

    @Override
    public List<Ticket> findAll() {
        return query("SELECT * FROM tickets ORDER BY id", ps -> {
        });
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        String sql = """
                SELECT * FROM tickets
                WHERE status = ?::ticket_status
                ORDER BY created_at DESC
                """;
        return query(sql, ps -> ps.setString(1, status.name()));
    }

    @Override
    public List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority) {
        String sql = """
                SELECT * FROM tickets
                WHERE status = ?::ticket_status
                  AND priority = ?::ticket_priority
                ORDER BY created_at DESC
                """;
        return query(sql, ps -> {
            ps.setString(1, status.name());
            ps.setString(2, priority.name());
        });
    }

    @Override
    public List<Ticket> findOverdue(LocalDateTime moment) {
        String sql = """
                SELECT * FROM tickets
                WHERE status IN ('NEW', 'IN_PROGRESS')
                  AND due_at < ?
                ORDER BY due_at
                """;
        return query(sql, ps -> ps.setTimestamp(1, Timestamp.valueOf(moment)));
    }

    @Override
    public List<Ticket> findPage(int page, int size) {
        String sql = """
                SELECT * FROM tickets
                ORDER BY id
                LIMIT ? OFFSET ?
                """;
        return query(sql, ps -> {
            ps.setInt(1, size);
            ps.setLong(2, (long) (page - 1) * size);
        });
    }

    @Override
    public long count() {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM tickets");
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка подсчёта заявок", e);
        }
    }

    @Override
    public void update(Ticket ticket) {
        String sql = """
                UPDATE tickets
                SET title = ?,
                    description = ?,
                    status = ?::ticket_status,
                    priority = ?::ticket_priority,
                    assignee_id = ?,
                    category_id = ?,
                    closed_at = ?,
                    due_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                RETURNING updated_at
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getStatus().name());
            ps.setString(4, ticket.getPriority().name());
            setNullableLong(ps, 5, ticket.getAssigneeId());
            setNullableLong(ps, 6, ticket.getCategoryId());
            setNullableDateTime(ps, 7, ticket.getClosedAt());
            setNullableDateTime(ps, 8, ticket.getDueAt());
            ps.setLong(9, ticket.getId());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new TicketNotFoundException(ticket.getId());
                }
                ticket.markUpdated(toLocalDateTime(rs.getTimestamp("updated_at")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось обновить заявку " + ticket.getId(), e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM tickets WHERE id = ?")) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось удалить заявку " + id, e);
        }
    }

    private List<Ticket> query(String sql, StatementBinder binder) {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            binder.bind(ps);
            List<Ticket> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTicket(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения заявок", e);
        }
    }

    private Ticket mapTicket(ResultSet rs) throws SQLException {
        return new Ticket(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("description"),
                TicketStatus.valueOf(rs.getString("status")),
                TicketPriority.valueOf(rs.getString("priority")),
                rs.getLong("customer_id"),
                getNullableLong(rs, "assignee_id"),
                getNullableLong(rs, "category_id"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                toLocalDateTime(rs.getTimestamp("closed_at")),
                toLocalDateTime(rs.getTimestamp("due_at"))
        );
    }
}
