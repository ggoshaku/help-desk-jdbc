package helpdesk.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TicketComment {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Long id;
    private final long ticketId;
    private final long authorId;
    private final String text;
    private final boolean internal;
    private LocalDateTime createdAt;
    private String authorName;
    private UserRole authorRole;

    public TicketComment(long ticketId, long authorId, String text, boolean internal) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Комментарий не может быть пустым");
        }
        this.ticketId = ticketId;
        this.authorId = authorId;
        this.text = text.strip();
        this.internal = internal;
    }

    public TicketComment(long id, long ticketId, long authorId, String text, boolean internal,
                         LocalDateTime createdAt, String authorName, UserRole authorRole) {
        this.id = id;
        this.ticketId = ticketId;
        this.authorId = authorId;
        this.text = text;
        this.internal = internal;
        this.createdAt = createdAt;
        this.authorName = authorName;
        this.authorRole = authorRole;
    }

    public void markSaved(long id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public long getTicketId() {
        return ticketId;
    }

    public long getAuthorId() {
        return authorId;
    }

    public String getText() {
        return text;
    }

    public boolean isInternal() {
        return internal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAuthorName() {
        return authorName;
    }

    public UserRole getAuthorRole() {
        return authorRole;
    }

    @Override
    public String toString() {
        return createdAt.format(FORMAT) + " " + authorName + " (" + authorRole + ")"
                + (internal ? " [внутренний]" : "") + ": " + text;
    }
}
