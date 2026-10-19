package helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record TicketStatusHistory(
        long id,
        long ticketId,
        TicketStatus oldStatus,
        TicketStatus newStatus,
        Long changedById,
        String changedByName,
        LocalDateTime changedAt
) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Override
    public String toString() {
        return changedAt.format(FORMAT) + "  " + oldStatus + " -> " + newStatus + "  (" + changedByName + ")";
    }
}
