package helpdesk.service;

import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.exception.UserNotFoundException;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;

import java.util.List;

public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public TicketService(TicketRepository ticketRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Ticket createTicket(String title, String description, TicketPriority priority,
                               long customerId, Long categoryId) {
        Ticket ticket = ticketRepository.save(new Ticket(title, description, priority, customerId, categoryId));
        notificationService.send("Заявка №" + ticket.getId() + " зарегистрирована");
        return ticket;
    }

    public Ticket changeStatus(long ticketId, TicketStatus newStatus, long userId) {
        User user = getUser(userId);
        Ticket ticket = getTicket(ticketId);
        if (!user.canChangeStatus(ticket, newStatus)) {
            throw new AccessDeniedException(
                    user.getName() + " не может перевести заявку №" + ticketId + " в " + newStatus
            );
        }

        Ticket updated = ticketRepository.changeStatus(ticketId, newStatus, userId);
        notificationService.send("Заявка №" + ticketId + ": " + ticket.getStatus() + " -> " + newStatus);
        return updated;
    }

    public Ticket assignTicket(long ticketId, long agentId, long assignedByUserId) {
        User agent = getUser(agentId);
        if (!(agent instanceof SupportAgent) || !agent.isActive()) {
            throw new AccessDeniedException(
                    "Заявку можно назначить только активному сотруднику поддержки, а " + agent.getName()
                            + " - " + agent.getRole()
            );
        }
        User assignedBy = getUser(assignedByUserId);
        Ticket ticket = getTicket(ticketId);
        if (!assignedBy.canChangeStatus(ticket, TicketStatus.IN_PROGRESS)) {
            throw new AccessDeniedException(assignedBy.getName() + " не может назначать заявки");
        }

        Ticket assigned = ticketRepository.assign(ticketId, agentId, assignedByUserId);
        notificationService.send("Заявка №" + ticketId + " назначена на " + agent.getName());
        return assigned;
    }

    public Ticket cancelTicket(long ticketId, long userId) {
        return changeStatus(ticketId, TicketStatus.CANCELLED, userId);
    }

    public Ticket getTicket(long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    public List<Ticket> findByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status);
    }

    private User getUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
