package helpdesk.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(long id) {
        super("Категория №" + id + " не найдена");
    }
}
