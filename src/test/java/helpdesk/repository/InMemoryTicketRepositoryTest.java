package helpdesk.repository;

import helpdesk.model.Ticket;
import helpdesk.model.TicketCategory;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTicketRepositoryTest {
    private TicketRepository repository;
    private Ticket wifi;
    private Ticket printer;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTicketRepository();
        wifi = new Ticket(1, "Не работает Wi-Fi", "", TicketPriority.HIGH, TicketCategory.NETWORK);
        printer = new Ticket(2, "Не печатает принтер", "", TicketPriority.LOW, TicketCategory.HARDWARE);
        repository.add(wifi);
        repository.add(printer);
    }

    @Test
    void findByIdReturnsEmptyOptionalForUnknownId() {
        assertTrue(repository.findById(99).isEmpty());
        assertEquals(wifi, repository.findById(1).orElseThrow());
    }

    @Test
    void findByStatusReturnsOnlyMatchingTickets() {
        wifi.startProcessing();

        assertEquals(List.of(printer), repository.findByStatus(TicketStatus.NEW));
        assertEquals(List.of(wifi), repository.findByStatus(TicketStatus.IN_PROGRESS));
    }

    @Test
    void findByPriorityAndCategory() {
        assertEquals(List.of(wifi), repository.findByPriority(TicketPriority.HIGH));
        assertEquals(List.of(printer), repository.findByCategory(TicketCategory.HARDWARE));
        assertTrue(repository.findByCategory(TicketCategory.ACCOUNT).isEmpty());
    }

    @Test
    void findAllReturnsCopy() {
        repository.findAll().clear();

        assertEquals(2, repository.findAll().size());
    }
}
