package helpdesk.service;

import helpdesk.model.AuditAction;
import helpdesk.model.AuditEntry;
import helpdesk.repository.AuditLogRepository;

import java.util.List;

public class AuditService {

    public static final String TICKET = "TICKET";
    public static final String COMMENT = "COMMENT";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void ticketAction(long userId, AuditAction action, long ticketId, String details) {
        auditLogRepository.add(userId, action, TICKET, ticketId, details);
    }

    public void commentAdded(long userId, long commentId, long ticketId) {
        auditLogRepository.add(userId, AuditAction.ADD_COMMENT, COMMENT, commentId, "заявка №" + ticketId);
    }

    public List<AuditEntry> ticketHistory(long ticketId) {
        return auditLogRepository.findByEntity(TICKET, ticketId);
    }
}
