package helpdesk.model;

import java.time.LocalDateTime;

public abstract class User {
    private Long id;
    private final String name;
    private final String email;
    private final Long departmentId;
    private final boolean active;
    private LocalDateTime createdAt;

    protected User(Long id, String name, String email, Long departmentId, boolean active, LocalDateTime createdAt) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Некорректный email: " + email);
        }
        this.id = id;
        this.name = name.strip();
        this.email = email.strip().toLowerCase();
        this.departmentId = departmentId;
        this.active = active;
        this.createdAt = createdAt;
    }

    public void markSaved(long id, LocalDateTime createdAt) {
        if (this.id != null) {
            throw new IllegalStateException("Пользователь уже сохранён с id " + this.id);
        }
        this.id = id;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public abstract UserRole getRole();

    public abstract void performAction();

    @Override
    public String toString() {
        return "#" + id + " " + name + " <" + email + "> " + getRole();
    }
}
