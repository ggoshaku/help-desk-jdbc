package helpdesk.model;

import java.time.LocalDateTime;

public class Administrator extends User {
    public Administrator(String name, String email, Long departmentId) {
        this(null, name, email, departmentId, true, null);
    }

    public Administrator(Long id, String name, String email, Long departmentId, boolean active, LocalDateTime createdAt) {
        super(id, name, email, departmentId, active, createdAt);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    @Override
    public void performAction() {
        System.out.println(getName() + " управляет системой");
    }
}
