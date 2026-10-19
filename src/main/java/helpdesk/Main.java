package helpdesk;

import helpdesk.config.DatabaseConfig;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.jdbc.JdbcTicketRepository;
import helpdesk.service.ConsoleNotificationService;
import helpdesk.service.TicketService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== HELP DESK ===");

        try (Connection connection = DatabaseConfig.getConnection()) {
            System.out.println("Подключение к PostgreSQL успешно");
            System.out.println("AutoCommit = " + connection.getAutoCommit());
        } catch (SQLException e) {
            System.out.println("Не удалось подключиться к PostgreSQL: " + e.getMessage());
            return;
        }

        TicketRepository ticketRepository = new JdbcTicketRepository(DatabaseConfig::getConnection);
        TicketService ticketService = new TicketService(ticketRepository, new ConsoleNotificationService());

        Ticket ticket = ticketService.createTicket(
                "Не печатает принтер",
                "Принтер в бухгалтерии пишет \"замятие бумаги\"",
                TicketPriority.MEDIUM,
                1,
                3L
        );
        System.out.println("Создана заявка с id " + ticket.getId() + ", created_at = " + ticket.getCreatedAt());
        System.out.println("findById: " + ticketService.getTicket(ticket.getId()));

        System.out.println();
        System.out.println("Все заявки:");
        printTickets(ticketService.findAll());

        System.out.println();
        System.out.println("Новые заявки:");
        printTickets(ticketService.findByStatus(TicketStatus.NEW));
    }

    private static void printTickets(List<Ticket> tickets) {
        tickets.forEach(System.out::println);
    }
}
