package helpdesk.model;

import helpdesk.exception.InvalidTicketStateException;

import java.time.LocalDateTime;
import java.util.Objects;

public class Ticket {
    private Long id;
    private final String title;
    private final String description;
    private TicketStatus status;
    private TicketPriority priority;
    private final long customerId;
    private Long assigneeId;
    private Long categoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private LocalDateTime dueAt;
    private long version;

    public Ticket(String title, String description, TicketPriority priority, long customerId, Long categoryId) {
        if (title == null || title.strip().length() < 3) {
            throw new IllegalArgumentException("Тема заявки должна содержать минимум 3 символа");
        }
        this.title = title.strip();
        this.description = Objects.requireNonNull(description, "description");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.customerId = customerId;
        this.categoryId = categoryId;
        this.status = TicketStatus.NEW;
    }

    public Ticket(long id, String title, String description, TicketStatus status, TicketPriority priority,
                  long customerId, Long assigneeId, Long categoryId,
                  LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime closedAt,
                  LocalDateTime dueAt, long version) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.customerId = customerId;
        this.assigneeId = assigneeId;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.closedAt = closedAt;
        this.dueAt = dueAt;
        this.version = version;
    }

    public void markSaved(long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (this.id != null) {
            throw new IllegalStateException("Заявка уже сохранена с id " + this.id);
        }
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void changeStatus(TicketStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidTicketStateException(
                    "Заявка №" + id + ": недопустимый переход " + status + " -> " + newStatus
            );
        }
        status = newStatus;
        if (newStatus == TicketStatus.CLOSED) {
            closedAt = LocalDateTime.now();
        }
    }

    public void assignTo(long agentId) {
        if (status == TicketStatus.NEW) {
            changeStatus(TicketStatus.IN_PROGRESS);
        } else if (status != TicketStatus.IN_PROGRESS) {
            throw new InvalidTicketStateException(
                    "Заявка №" + id + " в статусе " + status + ", назначить исполнителя нельзя"
            );
        }
        assigneeId = agentId;
    }

    public void setDeadline(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public boolean isOverdue(LocalDateTime moment) {
        return dueAt != null && status.isActive() && moment.isAfter(dueAt);
    }

    public void markUpdated(LocalDateTime updatedAt, long version) {
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public long getCustomerId() {
        return customerId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "#" + id + " " + title + " | " + status + " | " + priority;
    }
}
