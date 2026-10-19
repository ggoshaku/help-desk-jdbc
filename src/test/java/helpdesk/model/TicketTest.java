package helpdesk.model;

import helpdesk.exception.InvalidTicketStateException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketTest {

    @Test
    void newTicketHasNoIdAndStatusNew() {
        Ticket ticket = new Ticket("  Не работает Wi-Fi ", "Нет сети", TicketPriority.HIGH, 1, 1L);

        assertNull(ticket.getId());
        assertEquals(TicketStatus.NEW, ticket.getStatus());
        assertEquals("Не работает Wi-Fi", ticket.getTitle());
    }

    @Test
    void titleShorterThanThreeCharactersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Ticket("  ab ", "Описание", TicketPriority.LOW, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Ticket(null, "Описание", TicketPriority.LOW, 1, null));
    }

    @Test
    void ticketCanBeMarkedSavedOnlyOnce() {
        Ticket ticket = new Ticket("Ошибка 1С", "Падает отчёт", TicketPriority.MEDIUM, 1, 2L);
        LocalDateTime now = LocalDateTime.now();

        ticket.markSaved(10, now, now);

        assertEquals(10L, ticket.getId());
        assertThrows(IllegalStateException.class, () -> ticket.markSaved(11, now, now));
    }

    @Test
    void closingTicketSetsClosedAt() {
        Ticket ticket = new Ticket("Не работает VPN", "Нет доступа", TicketPriority.CRITICAL, 1, 4L);

        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        ticket.changeStatus(TicketStatus.RESOLVED);
        assertNull(ticket.getClosedAt());
        ticket.changeStatus(TicketStatus.CLOSED);

        assertEquals(TicketStatus.CLOSED, ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
    }

    @Test
    void invalidTransitionKeepsStatus() {
        Ticket ticket = new Ticket("Не работает VPN", "Нет доступа", TicketPriority.CRITICAL, 1, 4L);

        assertThrows(InvalidTicketStateException.class, () -> ticket.changeStatus(TicketStatus.CLOSED));
        assertEquals(TicketStatus.NEW, ticket.getStatus());
    }
}
