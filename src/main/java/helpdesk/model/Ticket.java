package helpdesk.model;

import helpdesk.exception.InvalidTicketStateException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Ticket {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final long id;
    private final String title;
    private final String description;
    private final TicketPriority priority;
    private final TicketCategory category;
    private final LocalDateTime createdAt;
    private final List<StatusChange> history = new ArrayList<>();
    private TicketStatus status;

    public Ticket(long id, String title, String description,
                  TicketPriority priority, TicketCategory category) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Название заявки не может быть пустым");
        }
        this.id = id;
        this.title = title.strip();
        this.description = description;
        this.priority = Objects.requireNonNull(priority, "priority");
        this.category = Objects.requireNonNull(category, "category");
        this.createdAt = LocalDateTime.now();
        this.status = TicketStatus.NEW;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public List<StatusChange> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public LocalDateTime getDeadline() {
        return createdAt.plus(priority.getSla());
    }

    public boolean isOverdue(LocalDateTime moment) {
        return status.isActive() && moment.isAfter(getDeadline());
    }

    public void startProcessing() {
        requireStatus(TicketStatus.NEW, "в работу можно взять только новую заявку");
        changeStatus(TicketStatus.IN_PROGRESS);
    }

    public void resolve() {
        requireStatus(TicketStatus.IN_PROGRESS, "решить можно только заявку в работе");
        changeStatus(TicketStatus.RESOLVED);
    }

    public void close() {
        requireStatus(TicketStatus.RESOLVED, "закрыть можно только решённую заявку");
        changeStatus(TicketStatus.CLOSED);
    }

    public void cancel() {
        if (status == TicketStatus.CLOSED || status == TicketStatus.CANCELLED) {
            throw invalidState("нельзя отменить закрытую или уже отменённую заявку");
        }
        changeStatus(TicketStatus.CANCELLED);
    }

    private void changeStatus(TicketStatus newStatus) {
        history.add(new StatusChange(status, newStatus, LocalDateTime.now()));
        status = newStatus;
    }

    private void requireStatus(TicketStatus expected, String message) {
        if (status != expected) {
            throw invalidState(message);
        }
    }

    private InvalidTicketStateException invalidState(String message) {
        return new InvalidTicketStateException(
                "Заявка №" + id + ": " + message + " (текущий статус " + status + ")"
        );
    }

    @Override
    public String toString() {
        return "#" + id + " " + title
                + " | " + status
                + " | " + priority
                + " | " + category.getTitle()
                + " | создана " + createdAt.format(DATE_FORMAT)
                + " | срок до " + getDeadline().format(DATE_FORMAT);
    }
}
