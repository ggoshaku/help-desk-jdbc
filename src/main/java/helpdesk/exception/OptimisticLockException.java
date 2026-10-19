package helpdesk.exception;

public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(long ticketId, long expectedVersion) {
        super("Заявка №" + ticketId + " уже изменена другим пользователем (ожидалась версия " + expectedVersion + ")");
    }
}
