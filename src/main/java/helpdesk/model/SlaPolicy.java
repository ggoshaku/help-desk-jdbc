package helpdesk.model;

import java.time.Duration;

public record SlaPolicy(long id, String name, TicketPriority priority, int responseMinutes, int resolveMinutes) {

    public Duration resolveTime() {
        return Duration.ofMinutes(resolveMinutes);
    }

    @Override
    public String toString() {
        return name + " (" + priority + "): реакция " + responseMinutes + " мин, решение " + resolveMinutes + " мин";
    }
}
