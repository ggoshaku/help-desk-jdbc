package helpdesk.repository.memory;

import helpdesk.exception.TicketNotFoundException;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class InMemoryTicketRepository implements TicketRepository {
    private final Map<Long, Ticket> tickets = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Ticket save(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        ticket.markSaved(nextId++, now, now);
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(long id) {
        return Optional.ofNullable(tickets.get(id));
    }

    @Override
    public Optional<Ticket> findByIdForUpdate(long id) {
        return findById(id);
    }

    @Override
    public List<Ticket> findAll() {
        return new ArrayList<>(tickets.values());
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        return findBy(ticket -> ticket.getStatus() == status);
    }

    @Override
    public List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority) {
        return findBy(ticket -> ticket.getStatus() == status && ticket.getPriority() == priority);
    }

    @Override
    public List<Ticket> findOverdue(LocalDateTime moment) {
        return tickets.values().stream()
                .filter(ticket -> ticket.isOverdue(moment))
                .sorted(Comparator.comparing(Ticket::getDueAt))
                .toList();
    }

    @Override
    public List<Ticket> findPage(int page, int size) {
        return tickets.values().stream()
                .skip((long) (page - 1) * size)
                .limit(size)
                .toList();
    }

    @Override
    public List<Ticket> search(String text) {
        String needle = text.toLowerCase();
        return findBy(ticket -> ticket.getTitle().toLowerCase().contains(needle)
                || ticket.getDescription().toLowerCase().contains(needle));
    }

    @Override
    public long count() {
        return tickets.size();
    }

    @Override
    public void update(Ticket ticket) {
        if (!tickets.containsKey(ticket.getId())) {
            throw new TicketNotFoundException(ticket.getId());
        }
        ticket.markUpdated(LocalDateTime.now(), ticket.getVersion() + 1);
        tickets.put(ticket.getId(), ticket);
    }

    @Override
    public boolean deleteById(long id) {
        return tickets.remove(id) != null;
    }

    private List<Ticket> findBy(Predicate<Ticket> condition) {
        return tickets.values().stream()
                .filter(condition)
                .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed())
                .toList();
    }
}
