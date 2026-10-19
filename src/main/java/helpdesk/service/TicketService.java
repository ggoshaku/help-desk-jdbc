package helpdesk.service;

import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.CategoryNotFoundException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.exception.UserNotFoundException;
import helpdesk.model.Category;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.CategoryRepository;
import helpdesk.repository.SlaPolicyRepository;
import helpdesk.repository.StatusHistoryRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.TransactionRunner;
import helpdesk.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

public class TicketService {

    private final TransactionRunner transactions;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatusHistoryRepository historyRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final NotificationService notificationService;

    public TicketService(TransactionRunner transactions,
                         TicketRepository ticketRepository,
                         UserRepository userRepository,
                         CategoryRepository categoryRepository,
                         StatusHistoryRepository historyRepository,
                         SlaPolicyRepository slaPolicyRepository,
                         NotificationService notificationService) {
        this.transactions = transactions;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.historyRepository = historyRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.notificationService = notificationService;
    }

    public Ticket createTicket(String title, String description, TicketPriority priority,
                               long customerId, Long categoryId) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CategoryNotFoundException(categoryId));
            if (!category.isActive()) {
                throw new IllegalArgumentException("Категория \"" + category.getName() + "\" неактивна");
            }
            if (priority == null) {
                priority = category.getDefaultPriority();
            }
        }
        if (priority == null) {
            priority = TicketPriority.MEDIUM;
        }

        Ticket ticket = new Ticket(title, description, priority, customerId, categoryId);
        slaPolicyRepository.findByPriority(priority)
                .ifPresent(policy -> ticket.setDeadline(LocalDateTime.now().plus(policy.resolveTime())));
        ticketRepository.save(ticket);
        notificationService.send("Заявка №" + ticket.getId() + " зарегистрирована");
        return ticket;
    }

    public Ticket changeStatus(long ticketId, TicketStatus newStatus, long userId) {
        User user = getUser(userId);

        Ticket ticket = transactions.inTransaction(() -> {
            Ticket locked = lockTicket(ticketId);
            if (!user.canChangeStatus(locked, newStatus)) {
                throw new AccessDeniedException(
                        user.getName() + " не может перевести заявку №" + ticketId + " в " + newStatus
                );
            }
            TicketStatus oldStatus = locked.getStatus();
            locked.changeStatus(newStatus);
            ticketRepository.update(locked);
            historyRepository.add(ticketId, oldStatus, newStatus, userId);
            return locked;
        });

        notificationService.send("Заявка №" + ticketId + " переведена в статус " + newStatus);
        return ticket;
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

        Ticket ticket = transactions.inTransaction(() -> {
            Ticket locked = lockTicket(ticketId);
            if (!assignedBy.canChangeStatus(locked, TicketStatus.IN_PROGRESS)) {
                throw new AccessDeniedException(assignedBy.getName() + " не может назначать заявки");
            }
            TicketStatus oldStatus = locked.getStatus();
            locked.assignTo(agentId);
            ticketRepository.update(locked);
            if (oldStatus != locked.getStatus()) {
                historyRepository.add(ticketId, oldStatus, locked.getStatus(), assignedByUserId);
            }
            return locked;
        });

        notificationService.send("Заявка №" + ticketId + " назначена на " + agent.getName());
        return ticket;
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

    public List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority) {
        return ticketRepository.findByStatusAndPriority(status, priority);
    }

    public List<Ticket> findOverdue(LocalDateTime moment) {
        return ticketRepository.findOverdue(moment);
    }

    public List<Ticket> findPage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Некорректные параметры страницы: page=" + page + ", size=" + size);
        }
        return ticketRepository.findPage(page, size);
    }

    public int countPages(int size) {
        return (int) ((ticketRepository.count() + size - 1) / size);
    }

    public List<Ticket> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return ticketRepository.search(text.strip());
    }

    private Ticket lockTicket(long ticketId) {
        return ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private User getUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
