package helpdesk.model;

public class Customer extends User {
    public Customer(long id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public void performAction() {
        System.out.println(getName() + " создаёт заявку");
    }
}
