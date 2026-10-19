package helpdesk.model;

import java.time.LocalDateTime;

public class Customer extends User {
    public Customer(String name, String email, Long departmentId) {
        this(null, name, email, departmentId, true, null);
    }

    public Customer(Long id, String name, String email, Long departmentId, boolean active, LocalDateTime createdAt) {
        super(id, name, email, departmentId, active, createdAt);
    }

    @Override
    public UserRole getRole() {
        return UserRole.CUSTOMER;
    }

    @Override
    public void performAction() {
        System.out.println(getName() + " создаёт заявку");
    }
}
