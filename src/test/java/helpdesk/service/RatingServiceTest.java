package helpdesk.service;

import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.RatingException;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketRating;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.memory.InMemoryAuditLogRepository;
import helpdesk.repository.memory.InMemoryRatingRepository;
import helpdesk.repository.memory.InMemoryTicketRepository;
import helpdesk.repository.memory.InMemoryTransactionRunner;
import helpdesk.repository.memory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingServiceTest {
    private final InMemoryTicketRepository tickets = new InMemoryTicketRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryRatingRepository ratings = new InMemoryRatingRepository();
    private final RatingService ratingService = new RatingService(new InMemoryTransactionRunner(), ratings, tickets,
            users, new AuditService(new InMemoryAuditLogRepository()));

    private User customer;
    private User otherCustomer;
    private User agent;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        customer = users.save(new Customer("Анна Петрова", "anna@example.org", null));
        otherCustomer = users.save(new Customer("Дмитрий Орлов", "dmitry@example.org", null));
        agent = users.save(new SupportAgent("Иван Соколов", "ivan@example.org", 1L));
        ticket = tickets.save(new Ticket("Не работает Wi-Fi", "Нет сети", TicketPriority.HIGH, customer.getId(), 1L));
        ticket.assignTo(agent.getId());
    }

    private void close() {
        ticket.changeStatus(TicketStatus.RESOLVED);
        ticket.changeStatus(TicketStatus.CLOSED);
    }

    @Test
    void customerRatesClosedTicket() {
        close();

        TicketRating rating = ratingService.rateTicket(ticket.getId(), customer.getId(), 4, "Быстро починили");

        assertEquals(agent.getId(), rating.getAgentId());
        assertEquals(1, ratingService.comments(rating.getId()).size());
    }

    @Test
    void openTicketCannotBeRated() {
        assertThrows(RatingException.class, () -> ratingService.rateTicket(ticket.getId(), customer.getId(), 5, null));
    }

    @Test
    void onlyAuthorCanRate() {
        close();

        assertThrows(AccessDeniedException.class,
                () -> ratingService.rateTicket(ticket.getId(), otherCustomer.getId(), 5, null));
    }

    @Test
    void ticketCanBeRatedOnlyOnce() {
        close();
        ratingService.rateTicket(ticket.getId(), customer.getId(), 5, null);

        assertThrows(RatingException.class, () -> ratingService.rateTicket(ticket.getId(), customer.getId(), 1, null));
    }

    @Test
    void scoreMustBeFromOneToFive() {
        close();

        assertThrows(IllegalArgumentException.class,
                () -> ratingService.rateTicket(ticket.getId(), customer.getId(), 6, null));
    }

    @Test
    void otherAgentCannotReplyToRating() {
        close();
        User elena = users.save(new SupportAgent("Елена Смирнова", "elena@example.org", 3L));
        TicketRating rating = ratingService.rateTicket(ticket.getId(), customer.getId(), 2, "Долго");

        assertThrows(AccessDeniedException.class,
                () -> ratingService.replyToRating(rating.getId(), elena.getId(), "Не моя заявка"));
        ratingService.replyToRating(rating.getId(), agent.getId(), "Разберёмся");
        assertEquals(2, ratingService.comments(rating.getId()).size());
    }
}
