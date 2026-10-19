package helpdesk.model;

public enum TicketCategory {
    NETWORK("Сеть"),
    SOFTWARE("Программное обеспечение"),
    HARDWARE("Оборудование"),
    ACCOUNT("Учётная запись");

    private final String title;

    TicketCategory(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
