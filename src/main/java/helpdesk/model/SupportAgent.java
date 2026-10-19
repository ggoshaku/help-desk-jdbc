package helpdesk.model;

import java.time.LocalDateTime;

public class SupportAgent extends User {
    public SupportAgent(String name, String email, Long departmentId) {
        this(null, name, email, departmentId, true, null);
    }

    public SupportAgent(Long id, String name, String email, Long departmentId, boolean active, LocalDateTime createdAt) {
        super(id, name, email, departmentId, active, createdAt);
    }

    @Override
    public UserRole getRole() {
        return UserRole.SUPPORT_AGENT;
    }

    @Override
    public void performAction() {
        System.out.println(getName() + " обрабатывает заявку");
    }
}
