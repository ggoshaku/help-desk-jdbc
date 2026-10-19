package helpdesk.repository;

import helpdesk.model.AuditAction;
import helpdesk.model.AuditEntry;

import java.util.List;

public interface AuditLogRepository {

    void add(long userId, AuditAction action, String entityType, long entityId, String details);

    List<AuditEntry> findByEntity(String entityType, long entityId);
}
