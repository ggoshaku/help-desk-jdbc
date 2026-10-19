package helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AuditEntry(
        long id,
        Long userId,
        String userName,
        AuditAction action,
        String entityType,
        long entityId,
        String details,
        LocalDateTime createdAt
) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Override
    public String toString() {
        return createdAt.format(FORMAT) + "  " + action + "  " + entityType + "#" + entityId
                + "  " + userName + (details == null ? "" : ": " + details);
    }
}
