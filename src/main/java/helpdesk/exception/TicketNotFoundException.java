package helpdesk.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(long id) {
        super("Заявка №" + id + " не найдена");
    }
}
