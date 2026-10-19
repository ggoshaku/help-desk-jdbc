package helpdesk.repository.memory;

import helpdesk.model.AssignmentChange;
import helpdesk.repository.AssignmentHistoryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InMemoryAssignmentHistoryRepository implements AssignmentHistoryRepository {
    private final List<AssignmentChange> history = new ArrayList<>();

    @Override
    public void add(long ticketId, Long oldAssigneeId, long newAssigneeId, long changedById) {
        history.add(new AssignmentChange(
                ticketId,
                oldAssigneeId == null ? null : "#" + oldAssigneeId,
                "#" + newAssigneeId,
                "#" + changedById,
                LocalDateTime.now()
        ));
    }

    @Override
    public List<AssignmentChange> findByTicketId(long ticketId) {
        return history.stream()
                .filter(change -> change.ticketId() == ticketId)
                .toList();
    }
}
