package helpdesk.model;

public enum TicketStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    public boolean isActive() {
        return this == NEW || this == IN_PROGRESS;
    }
}
