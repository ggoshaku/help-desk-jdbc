package helpdesk.service;

import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.model.Category;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.memory.InMemoryCategoryRepository;
import helpdesk.repository.memory.InMemorySlaPolicyRepository;
import helpdesk.repository.memory.InMemoryStatusHistoryRepository;
import helpdesk.repository.memory.InMemoryTicketRepository;
import helpdesk.repository.memory.InMemoryTransactionRunner;
import helpdesk.repository.memory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketServiceTest {
    private final List<String> messages = new ArrayList<>();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryCategoryRepository categories = new InMemoryCategoryRepository();
    private final InMemoryStatusHistoryRepository history = new InMemoryStatusHistoryRepository();
    private final TicketService ticketService = new TicketService(
            new InMemoryTransactionRunner(), new InMemoryTicketRepository(), users, categories, history,
            new InMemorySlaPolicyRepository(), messages::add);

    private User customer;
    private User agent;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        categories.add(new Category(1, "Сеть", "", TicketPriority.HIGH, true));
        categories.add(new Category(2, "Архив", "", TicketPriority.LOW, false));
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
    void assigningAgentMovesNewTicketToInProgress() {
        Ticket assigned = ticketService.assignTicket(ticket.getId(), agent.getId(), agent.getId());

        assertEquals(TicketStatus.IN_PROGRESS, assigned.getStatus());
        assertEquals(agent.getId(), assigned.getAssigneeId());
    }

    @Test
    void ticketCannotBeAssignedToCustomer() {
        assertThrows(AccessDeniedException.class,
                () -> ticketService.assignTicket(ticket.getId(), customer.getId(), agent.getId()));
        assertEquals(TicketStatus.NEW, ticketService.getTicket(ticket.getId()).getStatus());
    }

    @Test
    void agentMovesTicketThroughLifecycle() {
        ticketService.assignTicket(ticket.getId(), agent.getId(), agent.getId());
        ticketService.changeStatus(ticket.getId(), TicketStatus.RESOLVED, agent.getId());
        ticketService.changeStatus(ticket.getId(), TicketStatus.CLOSED, agent.getId());

        assertEquals(TicketStatus.CLOSED, ticketService.getTicket(ticket.getId()).getStatus());
        assertEquals(3, history.findByTicketId(ticket.getId()).size());
    }

    @Test
    void customerCanOnlyCancelOwnTicket() {
        assertThrows(AccessDeniedException.class,
                () -> ticketService.changeStatus(ticket.getId(), TicketStatus.IN_PROGRESS, customer.getId()));

        ticketService.cancelTicket(ticket.getId(), customer.getId());

        assertEquals(TicketStatus.CANCELLED, ticketService.getTicket(ticket.getId()).getStatus());
    }

    @Test
    void ticketInInactiveCategoryIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ticketService.createTicket("Старая система", "Ошибка", null, customer.getId(), 2L));
    }

    @Test
    void priorityDefaultsToCategoryPriority() {
        Ticket created = ticketService.createTicket("Нет VPN", "Не подключается", null, customer.getId(), 1L);

        assertEquals(TicketPriority.HIGH, created.getPriority());
    }

    @Test
    void deadlineIsCalculatedFromSla() {
        LocalDateTime before = LocalDateTime.now();
        Ticket created = ticketService.createTicket("Сервер недоступен", "Не отвечает", TicketPriority.CRITICAL,
                customer.getId(), 1L);

        assertTrue(created.getDueAt().isAfter(before.plusMinutes(29)));
        assertTrue(created.getDueAt().isBefore(LocalDateTime.now().plusMinutes(31)));
        assertEquals(List.of(created), ticketService.findOverdue(LocalDateTime.now().plusHours(1)));
    }
}
