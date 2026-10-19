package helpdesk.model;

public class Category {
    private final long id;
    private final String name;
    private final String description;
    private final TicketPriority defaultPriority;
    private final boolean active;

    public Category(long id, String name, String description, TicketPriority defaultPriority, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultPriority = defaultPriority;
        this.active = active;
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

    public TicketPriority getDefaultPriority() {
        return defaultPriority;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "#" + id + " " + name + " (" + defaultPriority + (active ? "" : ", неактивна") + ")";
    }
}
