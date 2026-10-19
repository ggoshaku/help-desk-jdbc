package helpdesk.ui;

import helpdesk.model.TicketDetails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TicketCardPrinter {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private TicketCardPrinter() {
    }

    public static String format(TicketDetails ticket) {
        return String.join(System.lineSeparator(),
                "+------------------------------------------------",
                "| Заявка №" + ticket.id() + ": " + ticket.title(),
                "| Статус:      " + ticket.status(),
                "| Приоритет:   " + ticket.priority(),
                "| Категория:   " + orDash(ticket.categoryName()),
                "| Клиент:      " + ticket.customerName(),
                "| Исполнитель: " + orDash(ticket.assigneeName()),
                "| Создана:     " + format(ticket.createdAt()),
                "| Изменена:    " + format(ticket.updatedAt()),
                "| Закрыта:     " + format(ticket.closedAt()),
                "+------------------------------------------------"
        );
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(FORMAT);
    }

    private static String orDash(String value) {
        return value == null ? "-" : value;
    }
}
