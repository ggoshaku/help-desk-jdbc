package helpdesk.service;

public class TelegramNotificationService implements NotificationService {
    private final String chatId;

    public TelegramNotificationService(String chatId) {
        this.chatId = chatId;
    }

    @Override
    public void send(String message) {
        System.out.println("[TELEGRAM -> " + chatId + "] " + message);
    }
}
