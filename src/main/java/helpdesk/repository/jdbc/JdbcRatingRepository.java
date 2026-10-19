package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.AgentRating;
import helpdesk.model.LowRatedTicket;
import helpdesk.model.RatingComment;
import helpdesk.model.TicketRating;
import helpdesk.repository.RatingRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static helpdesk.repository.jdbc.JdbcUtils.toLocalDateTime;

public class JdbcRatingRepository implements RatingRepository {

    private static final RowMapper<TicketRating> RATING_MAPPER = rs -> new TicketRating(
            rs.getLong("id"),
            rs.getLong("ticket_id"),
            rs.getLong("customer_id"),
            rs.getLong("agent_id"),
            rs.getInt("score"),
            toLocalDateTime(rs.getTimestamp("created_at"))
    );

    private final ConnectionProvider connections;

    public JdbcRatingRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public TicketRating save(TicketRating rating) {
        String sql = """
                INSERT INTO ticket_ratings(ticket_id, customer_id, agent_id, score)
                VALUES (?, ?, ?, ?)
                RETURNING id, created_at
                """;

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, rating.getTicketId());
            ps.setLong(2, rating.getCustomerId());
            ps.setLong(3, rating.getAgentId());
            ps.setInt(4, rating.getScore());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                rating.markSaved(rs.getLong("id"), toLocalDateTime(rs.getTimestamp("created_at")));
            }
            return rating;
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось сохранить оценку заявки " + rating.getTicketId(), e);
        }
    }

    @Override
    public Optional<TicketRating> findById(long id) {
        return query("SELECT * FROM ticket_ratings WHERE id = ?", ps -> ps.setLong(1, id), RATING_MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<TicketRating> findByTicketId(long ticketId) {
        return query("SELECT * FROM ticket_ratings WHERE ticket_id = ?", ps -> ps.setLong(1, ticketId), RATING_MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public void addComment(long ratingId, long authorId, String text) {
        String sql = "INSERT INTO rating_comments(rating_id, author_id, text) VALUES (?, ?, ?)";

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, ratingId);
            ps.setLong(2, authorId);
            ps.setString(3, text);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось добавить комментарий к оценке " + ratingId, e);
        }
    }

    @Override
    public List<RatingComment> findComments(long ratingId) {
        String sql = """
                SELECT rc.id, rc.rating_id, rc.author_id, u.name AS author_name, rc.text, rc.created_at
                FROM rating_comments rc
                JOIN users u ON u.id = rc.author_id
                WHERE rc.rating_id = ?
                ORDER BY rc.created_at, rc.id
                """;
        return query(sql, ps -> ps.setLong(1, ratingId), rs -> new RatingComment(
                rs.getLong("id"),
                rs.getLong("rating_id"),
                rs.getLong("author_id"),
                rs.getString("author_name"),
                rs.getString("text"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        ));
    }

    @Override
    public List<AgentRating> averageByAgent() {
        String sql = """
                SELECT u.name,
                       ROUND(AVG(r.score), 2) AS average_score,
                       COUNT(*) AS ratings_count
                FROM ticket_ratings r
                JOIN users u ON u.id = r.agent_id
                GROUP BY u.id, u.name
                ORDER BY average_score DESC, ratings_count DESC
                """;
        return query(sql, ps -> {
        }, rs -> new AgentRating(
                rs.getString("name"),
                rs.getDouble("average_score"),
                rs.getLong("ratings_count")
        ));
    }

    @Override
    public List<LowRatedTicket> findLowRated(int maxScore) {
        String sql = """
                SELECT t.id AS ticket_id,
                       t.title,
                       agent.name AS agent_name,
                       r.score,
                       last_comment.text AS last_comment
                FROM ticket_ratings r
                JOIN tickets t ON t.id = r.ticket_id
                JOIN users agent ON agent.id = r.agent_id
                LEFT JOIN LATERAL (
                    SELECT rc.text
                    FROM rating_comments rc
                    WHERE rc.rating_id = r.id
                    ORDER BY rc.created_at DESC, rc.id DESC
                    LIMIT 1
                ) last_comment ON TRUE
                WHERE r.score <= ?
                ORDER BY r.score, r.created_at DESC
                """;
        return query(sql, ps -> ps.setInt(1, maxScore), rs -> new LowRatedTicket(
                rs.getLong("ticket_id"),
                rs.getString("title"),
                rs.getString("agent_name"),
                rs.getInt("score"),
                rs.getString("last_comment")
        ));
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
            throw new DataAccessException("Ошибка чтения оценок", e);
        }
    }
}
