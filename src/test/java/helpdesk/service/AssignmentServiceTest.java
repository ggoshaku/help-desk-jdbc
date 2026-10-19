package helpdesk.service;

import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.InvalidTicketStateException;
import helpdesk.model.Administrator;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.memory.InMemoryAssignmentHistoryRepository;
import helpdesk.repository.memory.InMemoryAuditLogRepository;
import helpdesk.repository.memory.InMemoryStatusHistoryRepository;
import helpdesk.repository.memory.InMemoryTicketRepository;
import helpdesk.repository.memory.InMemoryTransactionRunner;
import helpdesk.repository.memory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssignmentServiceTest {
    private final InMemoryTicketRepository tickets = new InMemoryTicketRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryStatusHistoryRepository statusHistory = new InMemoryStatusHistoryRepository();
    private final InMemoryAssignmentHistoryRepository assignmentHistory = new InMemoryAssignmentHistoryRepository();
    private final AssignmentService assignmentService = new AssignmentService(
            new InMemoryTransactionRunner(), tickets, users, statusHistory, assignmentHistory,
            new AuditService(new InMemoryAuditLogRepository()), message -> {
    });

    private User customer;
    private User ivan;
    private User elena;
    private User admin;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        customer = users.save(new Customer("Анна Петрова", "anna@example.org", null));
        ivan = users.save(new SupportAgent("Иван Соколов", "ivan@example.org", 1L));
        elena = users.save(new SupportAgent("Елена Смирнова", "elena@example.org", 3L));
        admin = users.save(new Administrator("Олег Админов", "admin@example.org", 3L));
        ticket = tickets.save(new Ticket("Не работает Wi-Fi", "Нет сети", TicketPriority.HIGH, customer.getId(), 1L));
    }

    @Test
    void firstAssignmentMovesTicketToInProgress() {
        Ticket assigned = assignmentService.assignTicket(ticket.getId(), ivan.getId(), admin.getId());

        assertEquals(TicketStatus.IN_PROGRESS, assigned.getStatus());
        assertEquals(ivan.getId(), assigned.getAssigneeId());
        assertEquals(1, statusHistory.findByTicketId(ticket.getId()).size());
        assertEquals(1, assignmentHistory.findByTicketId(ticket.getId()).size());
    }

    @Test
    void reassignmentIsRecordedWithoutStatusChange() {
        assignmentService.assignTicket(ticket.getId(), ivan.getId(), admin.getId());
        assignmentService.assignTicket(ticket.getId(), elena.getId(), admin.getId());

        assertEquals(elena.getId(), tickets.findById(ticket.getId()).orElseThrow().getAssigneeId());
        assertEquals(2, assignmentHistory.findByTicketId(ticket.getId()).size());
        assertEquals(1, statusHistory.findByTicketId(ticket.getId()).size());
    }

    @Test
    void ticketCannotBeAssignedToCustomerOrAdmin() {
        assertThrows(AccessDeniedException.class,
                () -> assignmentService.assignTicket(ticket.getId(), customer.getId(), admin.getId()));
        assertThrows(AccessDeniedException.class,
                () -> assignmentService.assignTicket(ticket.getId(), admin.getId(), admin.getId()));
        assertEquals(TicketStatus.NEW, tickets.findById(ticket.getId()).orElseThrow().getStatus());
    }

    @Test
    void customerCannotAssignTickets() {
        assertThrows(AccessDeniedException.class,
                () -> assignmentService.assignTicket(ticket.getId(), ivan.getId(), customer.getId()));
    }

    @Test
    void closedTicketCannotBeAssigned() {
        ticket.changeStatus(TicketStatus.CANCELLED);

        assertThrows(InvalidTicketStateException.class,
                () -> assignmentService.assignTicket(ticket.getId(), ivan.getId(), admin.getId()));
    }
}
