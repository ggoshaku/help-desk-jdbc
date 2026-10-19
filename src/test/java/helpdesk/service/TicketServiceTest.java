package helpdesk.service;

import helpdesk.exception.InvalidTicketStateException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketCategory;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.repository.InMemoryTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketServiceTest {
    private final List<String> messages = new ArrayList<>();
    private final Customer customer = new Customer(1, "Анна Петрова", "anna@mail.ru");
    private final SupportAgent agent = new SupportAgent(2, "Сергей Иванов", "sergey@helpdesk.ru");

    private AuditService auditService;
    private TicketService ticketService;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        auditService = new AuditService();
        ticketService = new TicketService(new InMemoryTicketRepository(), messages::add, auditService);
        ticket = new Ticket(1, "Не работает Wi-Fi", "", TicketPriority.HIGH, TicketCategory.NETWORK);
        ticketService.registerTicket(ticket, customer);
    }

    @Test
    void sendsNotificationOnEverySuccessfulTransition() {
        ticketService.startTicket(ticket, agent);
        ticketService.resolveTicket(ticket, agent);
        ticketService.closeTicket(ticket, agent);

        assertEquals(4, messages.size());
        assertEquals("Заявка №1 закрыта", messages.get(3));
        assertEquals(4, auditService.getRecords().size());
    }

    @Test
    void doesNotNotifyWhenTransitionIsInvalid() {
        assertThrows(InvalidTicketStateException.class, () -> ticketService.closeTicket(ticket, agent));

        assertEquals(1, messages.size());
    }

    @Test
    void startTicketAssignsItToAgent() {
        ticketService.startTicket(ticket, agent);

        assertEquals(List.of(ticket), agent.getAssignedTickets());
    }

    @Test
    void getTicketThrowsForUnknownId() {
        assertEquals(ticket, ticketService.getTicket(1));
        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicket(42));
    }

    @Test
    void statisticsCountsTicketsByStatus() {
        Ticket second = new Ticket(2, "Ошибка приложения", "", TicketPriority.LOW, TicketCategory.SOFTWARE);
        ticketService.registerTicket(second, customer);
        ticketService.startTicket(ticket, agent);

        Map<TicketStatus, Long> statistics = ticketService.getStatistics();

        assertEquals(1, statistics.get(TicketStatus.NEW));
        assertEquals(1, statistics.get(TicketStatus.IN_PROGRESS));
        assertEquals(0, statistics.get(TicketStatus.CLOSED));
    }
}
