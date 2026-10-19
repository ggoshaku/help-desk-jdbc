package helpdesk.service;

import helpdesk.exception.TicketNotFoundException;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TicketService {
    private final TicketRepository repository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public TicketService(TicketRepository repository,
                         NotificationService notificationService,
                         AuditService auditService) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public void registerTicket(Ticket ticket, Customer customer) {
        repository.add(ticket);
        auditService.record(customer, "создал(а) заявку #" + ticket.getId());
        notificationService.send("Заявка №" + ticket.getId() + " от клиента " + customer.getName() + " зарегистрирована");
    }

    public Ticket getTicket(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    public void startTicket(Ticket ticket, SupportAgent agent) {
        agent.assignTicket(ticket);
        auditService.record(agent, "взял(а) в работу заявку #" + ticket.getId());
        notificationService.send("Заявка №" + ticket.getId() + " принята в работу");
    }

    public void resolveTicket(Ticket ticket, SupportAgent agent) {
        ticket.resolve();
        auditService.record(agent, "решил(а) заявку #" + ticket.getId());
        notificationService.send("По заявке №" + ticket.getId() + " найдено решение");
    }

    public void closeTicket(Ticket ticket, SupportAgent agent) {
        ticket.close();
        auditService.record(agent, "закрыл(а) заявку #" + ticket.getId());
        notificationService.send("Заявка №" + ticket.getId() + " закрыта");
    }

    public void cancelTicket(Ticket ticket, User initiator) {
        ticket.cancel();
        auditService.record(initiator, "отменил(а) заявку #" + ticket.getId());
        notificationService.send("Заявка №" + ticket.getId() + " отменена пользователем " + initiator.getName());
    }

    public Map<TicketStatus, Long> getStatistics() {
        Map<TicketStatus, Long> statistics = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            statistics.put(status, 0L);
        }
        for (Ticket ticket : repository.findAll()) {
            statistics.merge(ticket.getStatus(), 1L, Long::sum);
        }
        return statistics;
    }

    public List<Ticket> findOverdue(LocalDateTime moment) {
        return repository.findAll().stream()
                .filter(ticket -> ticket.isOverdue(moment))
                .toList();
    }
}
