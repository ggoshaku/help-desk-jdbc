package helpdesk.repository;

import helpdesk.model.TicketDetails;
import helpdesk.model.TicketStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReportRepository {

    Optional<TicketDetails> findDetails(long ticketId);

    List<TicketDetails> findAllDetails();

    Map<TicketStatus, Long> countByStatus();

    Map<String, Long> countByCategory();

    Map<String, Long> agentWorkload();
}
