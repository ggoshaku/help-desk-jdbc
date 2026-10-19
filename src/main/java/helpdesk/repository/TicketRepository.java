package helpdesk.repository;

import helpdesk.model.Ticket;
import helpdesk.model.TicketCategory;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    void add(Ticket ticket);

    List<Ticket> findAll();

    Optional<Ticket> findById(long id);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByPriority(TicketPriority priority);

    List<Ticket> findByCategory(TicketCategory category);
}
