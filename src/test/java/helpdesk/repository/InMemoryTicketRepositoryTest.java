package helpdesk.repository;

import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.repository.memory.InMemoryTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTicketRepositoryTest {
    private TicketRepository repository;
    private Ticket wifi;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTicketRepository();
        wifi = repository.save(new Ticket("Не работает Wi-Fi", "", TicketPriority.HIGH, 1, 1L));
    }

    @Test
    void saveAssignsId() {
        Ticket printer = repository.save(new Ticket("Не печатает принтер", "", TicketPriority.LOW, 1, 3L));

        assertEquals(1L, wifi.getId());
        assertEquals(2L, printer.getId());
    }

    @Test
    void findByIdReturnsEmptyOptionalForUnknownId() {
        assertTrue(repository.findById(99).isEmpty());
        assertEquals(wifi, repository.findById(1).orElseThrow());
    }

    @Test
    void findByStatusReturnsOnlyMatchingTickets() {
        assertEquals(List.of(wifi), repository.findByStatus(TicketStatus.NEW));
        assertTrue(repository.findByStatus(TicketStatus.CLOSED).isEmpty());
    }

    @Test
    void deleteByIdRemovesTicket() {
        assertTrue(repository.deleteById(1));
        assertFalse(repository.deleteById(1));
        assertTrue(repository.findAll().isEmpty());
    }
}
