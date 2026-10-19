package helpdesk.repository;

import helpdesk.model.SlaPolicy;
import helpdesk.model.TicketPriority;

import java.util.List;
import java.util.Optional;

public interface SlaPolicyRepository {

    List<SlaPolicy> findAll();

    Optional<SlaPolicy> findByPriority(TicketPriority priority);
}
