package helpdesk.service;

public class EmailNotificationService implements NotificationService {
    private final String recipient;

    public EmailNotificationService(String recipient) {
        this.recipient = recipient;
    }

    @Override
    public void send(String message) {
        System.out.println("[EMAIL -> " + recipient + "] " + message);
    }
}
