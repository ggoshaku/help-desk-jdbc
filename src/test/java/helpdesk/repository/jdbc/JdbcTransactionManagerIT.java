package helpdesk.repository.jdbc;

import helpdesk.config.DatabaseConfig;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JdbcTransactionManagerIT {
    private JdbcTransactionManager transactions;
    private JdbcTicketRepository tickets;
    private JdbcStatusHistoryRepository history;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        assumeTrue(System.getenv("DB_PASSWORD") != null, "DB_PASSWORD не задан, PostgreSQL недоступен");
        transactions = new JdbcTransactionManager(DatabaseConfig::getConnection);
        tickets = new JdbcTicketRepository(transactions);
        history = new JdbcStatusHistoryRepository(transactions);
        ticket = tickets.save(new Ticket("IT: транзакция", "integration test", TicketPriority.LOW, 1, null));
    }

    @AfterEach
    void cleanUp() {
        if (ticket != null) {
            tickets.deleteById(ticket.getId());
        }
    }

    @Test
    void commitStoresUpdateAndHistoryTogether() {
        transactions.runInTransaction(() -> {
            Ticket locked = tickets.findByIdForUpdate(ticket.getId()).orElseThrow();
            locked.changeStatus(TicketStatus.IN_PROGRESS);
            tickets.update(locked);
            history.add(ticket.getId(), TicketStatus.NEW, TicketStatus.IN_PROGRESS, 2);
        });

        assertEquals(TicketStatus.IN_PROGRESS, tickets.findById(ticket.getId()).orElseThrow().getStatus());
        assertEquals(1, history.findByTicketId(ticket.getId()).size());
    }

    @Test
    void exceptionRollsBackEveryStatement() {
        assertThrows(IllegalStateException.class, () -> transactions.runInTransaction(() -> {
            Ticket locked = tickets.findByIdForUpdate(ticket.getId()).orElseThrow();
            locked.changeStatus(TicketStatus.IN_PROGRESS);
            tickets.update(locked);
            history.add(ticket.getId(), TicketStatus.NEW, TicketStatus.IN_PROGRESS, 2);
            throw new IllegalStateException("сбой");
        }));

        assertEquals(TicketStatus.NEW, tickets.findById(ticket.getId()).orElseThrow().getStatus());
        assertEquals(0, history.findByTicketId(ticket.getId()).size());
    }

    @Test
    void foreignKeyViolationRollsBackStatusUpdate() {
        assertThrows(RuntimeException.class, () -> transactions.runInTransaction(() -> {
            Ticket locked = tickets.findByIdForUpdate(ticket.getId()).orElseThrow();
            locked.changeStatus(TicketStatus.IN_PROGRESS);
            tickets.update(locked);
            history.add(ticket.getId(), TicketStatus.NEW, TicketStatus.IN_PROGRESS, 999_999);
        }));

        assertEquals(TicketStatus.NEW, tickets.findById(ticket.getId()).orElseThrow().getStatus());
    }
}
