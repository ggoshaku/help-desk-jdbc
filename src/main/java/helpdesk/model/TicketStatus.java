package helpdesk.model;

public enum TicketStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    public boolean canTransitionTo(TicketStatus newStatus) {
        return switch (this) {
            case NEW -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == RESOLVED || newStatus == CANCELLED;
            case RESOLVED -> newStatus == CLOSED || newStatus == IN_PROGRESS;
            case CLOSED, CANCELLED -> false;
        };
    }

    public boolean isActive() {
        return this == NEW || this == IN_PROGRESS;
    }
}
