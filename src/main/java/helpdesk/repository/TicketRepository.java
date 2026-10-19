package helpdesk.repository;

import helpdesk.model.Ticket;
import helpdesk.model.TicketStatus;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(long id);

    List<Ticket> findAll();

    List<Ticket> findByStatus(TicketStatus status);

    void update(Ticket ticket);

    boolean deleteById(long id);
}
