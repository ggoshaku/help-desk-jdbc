package helpdesk.model;

import java.time.LocalDateTime;

public record TicketDetails(
        long id,
        String title,
        TicketStatus status,
        TicketPriority priority,
        String categoryName,
        String customerName,
        String assigneeName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt,
        LocalDateTime dueAt
) {
}
