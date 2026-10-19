package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.model.SlaPolicy;
import helpdesk.model.TicketPriority;
import helpdesk.repository.SlaPolicyRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSlaPolicyRepository implements SlaPolicyRepository {

    private static final RowMapper<SlaPolicy> MAPPER = rs -> new SlaPolicy(
            rs.getLong("id"),
            rs.getString("name"),
            TicketPriority.valueOf(rs.getString("priority")),
            rs.getInt("response_minutes"),
            rs.getInt("resolve_minutes")
    );

    private final ConnectionProvider connections;

    public JdbcSlaPolicyRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public List<SlaPolicy> findAll() {
        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM sla_policies ORDER BY resolve_minutes");
             ResultSet rs = ps.executeQuery()) {

            List<SlaPolicy> policies = new ArrayList<>();
            while (rs.next()) {
                policies.add(MAPPER.map(rs));
            }
            return policies;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения SLA", e);
        }
    }

    @Override
    public Optional<SlaPolicy> findByPriority(TicketPriority priority) {
        String sql = "SELECT * FROM sla_policies WHERE priority = ?::ticket_priority";

        try (Connection connection = connections.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, priority.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(MAPPER.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка поиска SLA для " + priority, e);
        }
    }
}
