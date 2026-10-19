package helpdesk.repository;

import helpdesk.model.AssignmentChange;

import java.util.List;

public interface AssignmentHistoryRepository {

    void add(long ticketId, Long oldAssigneeId, long newAssigneeId, long changedById);

    List<AssignmentChange> findByTicketId(long ticketId);
}
