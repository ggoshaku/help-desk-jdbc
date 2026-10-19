package helpdesk.model;

import java.time.Duration;

public enum TicketPriority {
    LOW(Duration.ofHours(24)),
    MEDIUM(Duration.ofHours(8)),
    HIGH(Duration.ofHours(2)),
    CRITICAL(Duration.ofMinutes(30));

    private final Duration sla;

    TicketPriority(Duration sla) {
        this.sla = sla;
    }

    public Duration getSla() {
        return sla;
    }
}
