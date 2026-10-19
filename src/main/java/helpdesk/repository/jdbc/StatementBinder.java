package helpdesk.repository.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
interface StatementBinder {
    void bind(PreparedStatement ps) throws SQLException;
}
