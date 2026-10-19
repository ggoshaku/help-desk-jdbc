package helpdesk.service;

import helpdesk.exception.TicketNotFoundException;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.repository.memory.InMemoryTicketRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketServiceTest {
    private final List<String> messages = new ArrayList<>();
    private final TicketService ticketService = new TicketService(new InMemoryTicketRepository(), messages::add);

    @Test
    void createTicketSavesItAndSendsNotification() {
        Ticket ticket = ticketService.createTicket("Не работает Wi-Fi", "Нет сети", TicketPriority.HIGH, 1, 1L);

        assertEquals(ticket, ticketService.getTicket(ticket.getId()));
        assertEquals(List.of("Заявка №1 зарегистрирована"), messages);
    }

    @Test
    void getTicketThrowsForUnknownId() {
        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicket(42));
    }
}
