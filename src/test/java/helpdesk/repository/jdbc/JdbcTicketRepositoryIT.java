package helpdesk.repository.jdbc;

import helpdesk.config.DatabaseConfig;
import helpdesk.exception.OptimisticLockException;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JdbcTicketRepositoryIT {
    private static final long CUSTOMER_ID = 1;

    private final List<Long> created = new ArrayList<>();
    private TicketRepository repository;

    @BeforeEach
    void setUp() {
        assumeTrue(System.getenv("DB_PASSWORD") != null, "DB_PASSWORD не задан, PostgreSQL недоступен");
        repository = new JdbcTicketRepository(DatabaseConfig::getConnection);
    }

    @AfterEach
    void cleanUp() {
        created.forEach(id -> repository.deleteById(id));
    }

    private Ticket save(String title) {
        Ticket ticket = repository.save(new Ticket(title, "integration test", TicketPriority.LOW, CUSTOMER_ID, 3L));
        created.add(ticket.getId());
        return ticket;
    }

    @Test
    void saveReturnsGeneratedIdAndTimestamps() {
        Ticket ticket = save("IT: сохранение");

        assertNotNull(ticket.getId());
        assertNotNull(ticket.getCreatedAt());
        Ticket loaded = repository.findById(ticket.getId()).orElseThrow();
        assertEquals("IT: сохранение", loaded.getTitle());
        assertEquals(TicketStatus.NEW, loaded.getStatus());
        assertEquals(3L, loaded.getCategoryId());
    }

    @Test
    void updateIncrementsVersion() {
        Ticket ticket = save("IT: версия");

        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        repository.update(ticket);

        assertEquals(1, ticket.getVersion());
        assertEquals(TicketStatus.IN_PROGRESS, repository.findById(ticket.getId()).orElseThrow().getStatus());
    }

    @Test
    void staleCopyCannotOverwriteNewerChanges() {
        Ticket ticket = save("IT: конфликт");
        Ticket stale = repository.findById(ticket.getId()).orElseThrow();

        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        repository.update(ticket);
        stale.changeStatus(TicketStatus.CANCELLED);

        assertThrows(OptimisticLockException.class, () -> repository.update(stale));
        assertEquals(TicketStatus.IN_PROGRESS, repository.findById(ticket.getId()).orElseThrow().getStatus());
    }

    @Test
    void searchTreatsPercentAsPlainText() {
        save("IT: загрузка 100% CPU");
        save("IT: загрузка 10 CPU");

        assertEquals(1, repository.search("100%").size());
    }

    @Test
    void deleteByIdRemovesTicket() {
        Ticket ticket = save("IT: удаление");

        assertTrue(repository.deleteById(ticket.getId()));
        assertFalse(repository.findById(ticket.getId()).isPresent());
    }
}
