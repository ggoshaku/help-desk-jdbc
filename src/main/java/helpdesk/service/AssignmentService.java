package helpdesk.service;

import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.exception.UserNotFoundException;
import helpdesk.model.AssignmentChange;
import helpdesk.model.AuditAction;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.AssignmentHistoryRepository;
import helpdesk.repository.StatusHistoryRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.TransactionRunner;
import helpdesk.repository.UserRepository;

import java.util.List;
import java.util.Objects;

public class AssignmentService {

    private final TransactionRunner transactions;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final AssignmentHistoryRepository assignmentHistoryRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AssignmentService(TransactionRunner transactions,
                             TicketRepository ticketRepository,
                             UserRepository userRepository,
                             StatusHistoryRepository statusHistoryRepository,
                             AssignmentHistoryRepository assignmentHistoryRepository,
                             AuditService auditService,
                             NotificationService notificationService) {
        this.transactions = transactions;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.assignmentHistoryRepository = assignmentHistoryRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
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
            Ticket locked = ticketRepository.findByIdForUpdate(ticketId)
                    .orElseThrow(() -> new TicketNotFoundException(ticketId));
            if (!assignedBy.canChangeStatus(locked, TicketStatus.IN_PROGRESS)) {
                throw new AccessDeniedException(assignedBy.getName() + " не может назначать заявки");
            }
            if (Objects.equals(locked.getAssigneeId(), agentId)) {
                return locked;
            }

            TicketStatus oldStatus = locked.getStatus();
            Long oldAssigneeId = locked.getAssigneeId();
            locked.assignTo(agentId);
            ticketRepository.update(locked);
            assignmentHistoryRepository.add(ticketId, oldAssigneeId, agentId, assignedByUserId);
            auditService.ticketAction(assignedByUserId, AuditAction.ASSIGN_TICKET, ticketId, agent.getName());
            if (oldStatus != locked.getStatus()) {
                statusHistoryRepository.add(ticketId, oldStatus, locked.getStatus(), assignedByUserId);
            }
            return locked;
        });

        notificationService.send("Заявка №" + ticketId + " назначена на " + agent.getName());
        return ticket;
    }

    public List<AssignmentChange> history(long ticketId) {
        return assignmentHistoryRepository.findByTicketId(ticketId);
    }

    private User getUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
