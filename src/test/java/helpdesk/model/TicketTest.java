package helpdesk.model;

import helpdesk.exception.InvalidTicketStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketTest {
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticket = new Ticket(1, "Не работает Wi-Fi", "Нет сети", TicketPriority.HIGH, TicketCategory.NETWORK);
    }

    @Test
    void newTicketHasStatusNew() {
        assertEquals(TicketStatus.NEW, ticket.getStatus());
        assertTrue(ticket.getHistory().isEmpty());
    }

    @Test
    void ticketGoesThroughFullLifecycle() {
        ticket.startProcessing();
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());

        ticket.resolve();
        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());

        ticket.close();
        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
    }

    @Test
    void cannotResolveNewTicket() {
        assertThrows(InvalidTicketStateException.class, ticket::resolve);
        assertEquals(TicketStatus.NEW, ticket.getStatus());
    }

    @Test
    void cannotCloseTicketThatIsNotResolved() {
        ticket.startProcessing();

        assertThrows(InvalidTicketStateException.class, ticket::close);
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
    }

    @Test
    void cannotStartProcessingTwice() {
        ticket.startProcessing();

        assertThrows(InvalidTicketStateException.class, ticket::startProcessing);
    }

    @Test
    void newTicketCanBeCancelled() {
        ticket.cancel();

        assertEquals(TicketStatus.CANCELLED, ticket.getStatus());
        assertThrows(InvalidTicketStateException.class, ticket::startProcessing);
    }

    @Test
    void cannotCancelClosedTicket() {
        ticket.startProcessing();
        ticket.resolve();
        ticket.close();

        assertThrows(InvalidTicketStateException.class, ticket::cancel);
        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
    }

    @Test
    void blankTitleIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Ticket(2, "  ", "Описание", TicketPriority.LOW, TicketCategory.SOFTWARE));
        assertThrows(IllegalArgumentException.class,
                () -> new Ticket(3, null, "Описание", TicketPriority.LOW, TicketCategory.SOFTWARE));
    }

    @Test
    void historyContainsEveryTransition() {
        ticket.startProcessing();
        ticket.resolve();
        ticket.close();

        List<StatusChange> history = ticket.getHistory();
        assertEquals(3, history.size());
        assertEquals(TicketStatus.NEW, history.get(0).from());
        assertEquals(TicketStatus.IN_PROGRESS, history.get(0).to());
        assertEquals(TicketStatus.CLOSED, history.get(2).to());
    }

    @Test
    void historyCannotBeModifiedFromOutside() {
        ticket.startProcessing();

        assertThrows(UnsupportedOperationException.class, () -> ticket.getHistory().clear());
    }

    @Test
    void deadlineDependsOnPriority() {
        assertEquals(ticket.getCreatedAt().plusHours(2), ticket.getDeadline());
    }

    @Test
    void activeTicketBecomesOverdueAfterDeadline() {
        assertFalse(ticket.isOverdue(ticket.getCreatedAt().plusHours(1)));
        assertTrue(ticket.isOverdue(ticket.getCreatedAt().plusHours(3)));
    }

    @Test
    void resolvedTicketIsNotOverdue() {
        ticket.startProcessing();
        ticket.resolve();

        assertFalse(ticket.isOverdue(ticket.getCreatedAt().plusDays(1)));
    }
}
