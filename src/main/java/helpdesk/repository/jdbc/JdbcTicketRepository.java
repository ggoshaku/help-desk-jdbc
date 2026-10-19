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
                    customer_id, assignee_id, category_id
                )
                VALUES (?, ?, ?::ticket_status, ?::ticket_priority, ?, ?, ?)
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
        String sql = "SELECT * FROM tickets WHERE id = ?";

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
    public void update(Ticket ticket) {
        try (Connection connection = connections.getConnection()) {
            update(connection, ticket);
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось обновить заявку " + ticket.getId(), e);
        }
    }

    @Override
    public Ticket changeStatus(long ticketId, TicketStatus newStatus, long changedByUserId) {
        String selectSql = "SELECT * FROM tickets WHERE id = ? FOR UPDATE";

        String historySql = """
                INSERT INTO ticket_status_history(
                    ticket_id, old_status, new_status, changed_by_id
                )
                VALUES (?, ?::ticket_status, ?::ticket_status, ?)
                """;

        try (Connection connection = connections.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Ticket ticket;
                try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                    ps.setLong(1, ticketId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new TicketNotFoundException(ticketId);
                        }
                        ticket = mapTicket(rs);
                    }
                }

                TicketStatus oldStatus = ticket.getStatus();
                ticket.changeStatus(newStatus);
                update(connection, ticket);

                try (PreparedStatement ps = connection.prepareStatement(historySql)) {
                    ps.setLong(1, ticketId);
                    ps.setString(2, oldStatus.name());
                    ps.setString(3, newStatus.name());
                    ps.setLong(4, changedByUserId);
                    ps.executeUpdate();
                }

                connection.commit();
                return ticket;
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка изменения статуса заявки " + ticketId, e);
        }
    }

    private void update(Connection connection, Ticket ticket) throws SQLException {
        String sql = """
                UPDATE tickets
                SET title = ?,
                    description = ?,
                    status = ?::ticket_status,
                    priority = ?::ticket_priority,
                    assignee_id = ?,
                    category_id = ?,
                    closed_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                RETURNING updated_at
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getStatus().name());
            ps.setString(4, ticket.getPriority().name());
            setNullableLong(ps, 5, ticket.getAssigneeId());
            setNullableLong(ps, 6, ticket.getCategoryId());
            setNullableDateTime(ps, 7, ticket.getClosedAt());
            ps.setLong(8, ticket.getId());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new TicketNotFoundException(ticket.getId());
                }
                ticket.markUpdated(toLocalDateTime(rs.getTimestamp("updated_at")));
            }
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
                toLocalDateTime(rs.getTimestamp("closed_at"))
        );
    }
}
