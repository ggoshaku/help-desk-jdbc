package helpdesk.model;

public class Administrator extends User {
    public Administrator(long id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public void performAction() {
        System.out.println(getName() + " управляет системой");
    }
}
