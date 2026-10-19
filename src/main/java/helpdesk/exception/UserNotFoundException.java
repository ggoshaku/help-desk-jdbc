package helpdesk.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(long id) {
        super("Пользователь №" + id + " не найден");
    }
}
