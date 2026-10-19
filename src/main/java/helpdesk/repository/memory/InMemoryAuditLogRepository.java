package helpdesk.repository.memory;

import helpdesk.model.AuditAction;
import helpdesk.model.AuditEntry;
import helpdesk.repository.AuditLogRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InMemoryAuditLogRepository implements AuditLogRepository {
    private final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public void add(long userId, AuditAction action, String entityType, long entityId, String details) {
        entries.add(new AuditEntry(
                entries.size() + 1L, userId, null, action, entityType, entityId, details, LocalDateTime.now()
        ));
    }

    @Override
    public List<AuditEntry> findByEntity(String entityType, long entityId) {
        return entries.stream()
                .filter(entry -> entry.entityType().equals(entityType) && entry.entityId() == entityId)
                .toList();
    }
}
