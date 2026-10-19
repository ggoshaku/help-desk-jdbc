package helpdesk.service;

import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.memory.InMemoryTicketRepository;
import helpdesk.repository.memory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketServiceTest {
    private final List<String> messages = new ArrayList<>();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final TicketService ticketService =
            new TicketService(new InMemoryTicketRepository(), users, messages::add);

    private User customer;
    private User agent;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        customer = users.save(new Customer("Анна Петрова", "anna@example.org", null));
        agent = users.save(new SupportAgent("Иван Соколов", "ivan@example.org", 1L));
        ticket = ticketService.createTicket("Не работает Wi-Fi", "Нет сети", TicketPriority.HIGH, customer.getId(), 1L);
    }

    @Test
    void createTicketSavesItAndSendsNotification() {
        assertEquals(ticket, ticketService.getTicket(ticket.getId()));
        assertEquals(List.of("Заявка №1 зарегистрирована"), messages);
    }

    @Test
    void getTicketThrowsForUnknownId() {
        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicket(42));
    }

    @Test
    void agentMovesTicketThroughLifecycle() {
        ticketService.changeStatus(ticket.getId(), TicketStatus.IN_PROGRESS, agent.getId());
        ticketService.changeStatus(ticket.getId(), TicketStatus.RESOLVED, agent.getId());
        ticketService.changeStatus(ticket.getId(), TicketStatus.CLOSED, agent.getId());

        assertEquals(TicketStatus.CLOSED, ticketService.getTicket(ticket.getId()).getStatus());
    }

    @Test
    void customerCanOnlyCancelOwnTicket() {
        assertThrows(AccessDeniedException.class,
                () -> ticketService.changeStatus(ticket.getId(), TicketStatus.IN_PROGRESS, customer.getId()));

        ticketService.cancelTicket(ticket.getId(), customer.getId());

        assertEquals(TicketStatus.CANCELLED, ticketService.getTicket(ticket.getId()).getStatus());
    }
}
