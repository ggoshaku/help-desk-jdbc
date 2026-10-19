package helpdesk;

import helpdesk.config.DatabaseConfig;
import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.InvalidTicketStateException;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.CategoryRepository;
import helpdesk.repository.StatusHistoryRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;
import helpdesk.repository.jdbc.JdbcCategoryRepository;
import helpdesk.repository.jdbc.JdbcStatusHistoryRepository;
import helpdesk.repository.jdbc.JdbcTicketRepository;
import helpdesk.repository.jdbc.JdbcUserRepository;
import helpdesk.service.ConsoleNotificationService;
import helpdesk.service.TicketService;

import java.sql.Connection;
import java.sql.SQLException;

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

        UserRepository userRepository = new JdbcUserRepository(DatabaseConfig::getConnection);
        CategoryRepository categoryRepository = new JdbcCategoryRepository(DatabaseConfig::getConnection);
        TicketRepository ticketRepository = new JdbcTicketRepository(DatabaseConfig::getConnection);
        StatusHistoryRepository historyRepository = new JdbcStatusHistoryRepository(DatabaseConfig::getConnection);
        TicketService ticketService =
                new TicketService(ticketRepository, userRepository, new ConsoleNotificationService());

        User customer = userRepository.findByEmail("anna@example.org").orElseThrow();
        User agent = userRepository.findByEmail("ivan.support@example.org").orElseThrow();
        User admin = userRepository.findByEmail("admin@example.org").orElseThrow();
        System.out.println("Категории:");
        categoryRepository.findAll().forEach(System.out::println);

        Ticket ticket = ticketService.createTicket(
                "Не печатает принтер",
                "Принтер в бухгалтерии пишет \"замятие бумаги\"",
                TicketPriority.MEDIUM,
                customer.getId(),
                3L
        );
        printTicket(ticket);

        try {
            ticketService.assignTicket(ticket.getId(), customer.getId(), admin.getId());
        } catch (AccessDeniedException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
        ticket = ticketService.assignTicket(ticket.getId(), agent.getId(), admin.getId());
        printTicket(ticket);
        System.out.println("Исполнитель: " + ticket.getAssigneeId());
        ticket = ticketService.changeStatus(ticket.getId(), TicketStatus.RESOLVED, agent.getId());
        printTicket(ticket);

        System.out.println("История статусов:");
        historyRepository.findByTicketId(ticket.getId()).forEach(System.out::println);

        try {
            ticketService.changeStatus(ticket.getId(), TicketStatus.CLOSED, customer.getId());
        } catch (AccessDeniedException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        Ticket mistake = ticketService.createTicket(
                "Ошибочная заявка", "Создана по ошибке", TicketPriority.LOW, customer.getId(), null
        );
        printTicket(ticketService.cancelTicket(mistake.getId(), customer.getId()));
        try {
            ticketService.cancelTicket(mistake.getId(), customer.getId());
        } catch (InvalidTicketStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void printTicket(Ticket ticket) {
        System.out.println(ticket + " | updated_at = " + ticket.getUpdatedAt());
    }
}
