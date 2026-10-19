package helpdesk.repository;

import helpdesk.model.TicketStatus;
import helpdesk.model.TicketStatusHistory;

import java.util.List;

public interface StatusHistoryRepository {

    void add(long ticketId, TicketStatus oldStatus, TicketStatus newStatus, long changedById);

    List<TicketStatusHistory> findByTicketId(long ticketId);
}
