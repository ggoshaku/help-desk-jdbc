package helpdesk.repository;

import helpdesk.model.Ticket;
import helpdesk.model.TicketCategory;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class InMemoryTicketRepository implements TicketRepository {
    private final List<Ticket> tickets = new ArrayList<>();

    @Override
    public void add(Ticket ticket) {
        tickets.add(ticket);
    }

    @Override
    public List<Ticket> findAll() {
        return new ArrayList<>(tickets);
    }

    @Override
    public Optional<Ticket> findById(long id) {
        return tickets.stream()
                .filter(ticket -> ticket.getId() == id)
                .findFirst();
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        return findBy(ticket -> ticket.getStatus() == status);
    }

    @Override
    public List<Ticket> findByPriority(TicketPriority priority) {
        return findBy(ticket -> ticket.getPriority() == priority);
    }

    @Override
    public List<Ticket> findByCategory(TicketCategory category) {
        return findBy(ticket -> ticket.getCategory() == category);
    }

    private List<Ticket> findBy(Predicate<Ticket> condition) {
        return tickets.stream()
                .filter(condition)
                .toList();
    }
}
