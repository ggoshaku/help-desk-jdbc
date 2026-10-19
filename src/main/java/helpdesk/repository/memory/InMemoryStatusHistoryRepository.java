package helpdesk.repository.memory;

import helpdesk.model.TicketStatus;
import helpdesk.model.TicketStatusHistory;
import helpdesk.repository.StatusHistoryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InMemoryStatusHistoryRepository implements StatusHistoryRepository {
    private final List<TicketStatusHistory> history = new ArrayList<>();

    @Override
    public void add(long ticketId, TicketStatus oldStatus, TicketStatus newStatus, long changedById) {
        history.add(new TicketStatusHistory(
                history.size() + 1L, ticketId, oldStatus, newStatus, changedById, null, LocalDateTime.now()
        ));
    }

    @Override
    public List<TicketStatusHistory> findByTicketId(long ticketId) {
        return history.stream()
                .filter(entry -> entry.ticketId() == ticketId)
                .toList();
    }
}
