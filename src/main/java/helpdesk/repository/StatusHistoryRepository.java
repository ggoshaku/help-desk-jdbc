package helpdesk.repository;

import helpdesk.model.TicketStatusHistory;

import java.util.List;

public interface StatusHistoryRepository {

    List<TicketStatusHistory> findByTicketId(long ticketId);
}
