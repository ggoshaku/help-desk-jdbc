package helpdesk.model;

import java.time.LocalDateTime;

public class TicketRating {
    private Long id;
    private final long ticketId;
    private final long customerId;
    private final long agentId;
    private final int score;
    private LocalDateTime createdAt;

    public TicketRating(long ticketId, long customerId, long agentId, int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Оценка должна быть от 1 до 5, получено " + score);
        }
        this.ticketId = ticketId;
        this.customerId = customerId;
        this.agentId = agentId;
        this.score = score;
    }

    public TicketRating(long id, long ticketId, long customerId, long agentId, int score, LocalDateTime createdAt) {
        this(ticketId, customerId, agentId, score);
        this.id = id;
        this.createdAt = createdAt;
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

    public long getCustomerId() {
        return customerId;
    }

    public long getAgentId() {
        return agentId;
    }

    public int getScore() {
        return score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Оценка заявки №" + ticketId + ": " + "★".repeat(score) + "☆".repeat(5 - score);
    }
}
