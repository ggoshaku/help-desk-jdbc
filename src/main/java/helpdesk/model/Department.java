package helpdesk.model;

import java.time.LocalDateTime;

public class Department {
    private final long id;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;

    public Department(long id, String name, String description, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "#" + id + " " + name;
    }
}
