package helpdesk.repository.jdbc;

import helpdesk.exception.DataAccessException;
import helpdesk.repository.TransactionRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public class JdbcTransactionManager implements TransactionRunner, ConnectionProvider {

    private final ConnectionProvider dataSource;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public JdbcTransactionManager(ConnectionProvider dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = current.get();
        return connection == null ? dataSource.getConnection() : nonClosing(connection);
    }

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        if (current.get() != null) {
            return action.get();
        }

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            current.set(connection);
            try {
                T result = action.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка выполнения транзакции", e);
        }
    }

    private static void rollback(Connection connection, RuntimeException cause) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            cause.addSuppressed(e);
        }
    }

    private static Connection nonClosing(Connection connection) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        return null;
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
        );
    }
}
