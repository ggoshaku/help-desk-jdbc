package helpdesk.repository;

import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(long id);

    List<Ticket> findAll();

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority);

    void update(Ticket ticket);

    Ticket changeStatus(long ticketId, TicketStatus newStatus, long changedByUserId);

    Ticket assign(long ticketId, long agentId, long assignedByUserId);

    boolean deleteById(long id);
}
