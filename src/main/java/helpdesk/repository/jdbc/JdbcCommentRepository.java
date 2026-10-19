package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.TicketComment;
import helpdesk.model.UserRole;
import helpdesk.repository.CommentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static helpdesk.repository.jdbc.JdbcUtils.toLocalDateTime;

public class JdbcCommentRepository implements CommentRepository {

    private static final String SELECT_BY_TICKET = """
            SELECT c.id,
                   c.ticket_id,
                   c.author_id,
                   c.text,
                   c.internal,
                   c.created_at,
                   u.name AS author_name,
                   u.role AS author_role
            FROM ticket_comments c
            JOIN users u ON u.id = c.author_id
            WHERE c.ticket_id = ?
            """;

    private final ConnectionProvider connections;

    public JdbcCommentRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public TicketComment add(TicketComment comment) {
        String sql = """
                INSERT INTO ticket_comments(
                    ticket_id, author_id, text, internal
                )
                VALUES (?, ?, ?, ?)
                RETURNING id, created_at
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, comment.getTicketId());
            ps.setLong(2, comment.getAuthorId());
            ps.setString(3, comment.getText());
            ps.setBoolean(4, comment.isInternal());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                comment.markSaved(rs.getLong("id"), toLocalDateTime(rs.getTimestamp("created_at")));
            }
            return comment;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось добавить комментарий к заявке " + comment.getTicketId(), e);
        }
    }

    @Override
    public List<TicketComment> findByTicketId(long ticketId) {
        return query(SELECT_BY_TICKET + " ORDER BY c.created_at, c.id", ticketId);
    }

    @Override
    public List<TicketComment> findExternalByTicketId(long ticketId) {
        return query(SELECT_BY_TICKET + " AND NOT c.internal ORDER BY c.created_at, c.id", ticketId);
    }

    private List<TicketComment> query(String sql, long ticketId) {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, ticketId);
            List<TicketComment> comments = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(new TicketComment(
                            rs.getLong("id"),
                            rs.getLong("ticket_id"),
                            rs.getLong("author_id"),
                            rs.getString("text"),
                            rs.getBoolean("internal"),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            rs.getString("author_name"),
                            UserRole.valueOf(rs.getString("author_role"))
                    ));
                }
            }
            return comments;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения комментариев заявки " + ticketId, e);
        }
    }
}
