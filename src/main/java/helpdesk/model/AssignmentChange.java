package helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentChange(
        long ticketId,
        String oldAssigneeName,
        String newAssigneeName,
        String changedByName,
        LocalDateTime changedAt
) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Override
    public String toString() {
        return changedAt.format(FORMAT) + "  " + (oldAssigneeName == null ? "-" : oldAssigneeName)
                + " -> " + newAssigneeName + "  (" + changedByName + ")";
    }
}
