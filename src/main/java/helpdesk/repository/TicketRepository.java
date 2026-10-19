package helpdesk.repository;

import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(long id);

    Optional<Ticket> findByIdForUpdate(long id);

    List<Ticket> findAll();

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority);

    List<Ticket> findOverdue(LocalDateTime moment);

    List<Ticket> findPage(int page, int size);

    List<Ticket> search(String text);

    List<Ticket> fullTextSearch(String query);

    long count();

    void update(Ticket ticket);

    boolean deleteById(long id);
}
