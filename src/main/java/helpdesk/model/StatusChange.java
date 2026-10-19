package helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record StatusChange(TicketStatus from, TicketStatus to, LocalDateTime changedAt) {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Override
    public String toString() {
        return changedAt.format(TIME_FORMAT) + "  " + from + " -> " + to;
    }
}
