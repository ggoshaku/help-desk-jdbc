package helpdesk;

import helpdesk.config.DatabaseConfig;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.CategoryRepository;
import helpdesk.repository.DepartmentRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;
import helpdesk.repository.jdbc.JdbcCategoryRepository;
import helpdesk.repository.jdbc.JdbcDepartmentRepository;
import helpdesk.repository.jdbc.JdbcTicketRepository;
import helpdesk.repository.jdbc.JdbcUserRepository;
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

        UserRepository userRepository = new JdbcUserRepository(DatabaseConfig::getConnection);
        CategoryRepository categoryRepository = new JdbcCategoryRepository(DatabaseConfig::getConnection);
        DepartmentRepository departmentRepository = new JdbcDepartmentRepository(DatabaseConfig::getConnection);
        TicketRepository ticketRepository = new JdbcTicketRepository(DatabaseConfig::getConnection);
        TicketService ticketService = new TicketService(ticketRepository, new ConsoleNotificationService());

        System.out.println("Подразделения:");
        departmentRepository.findAll().forEach(System.out::println);
        System.out.println("Категории:");
        categoryRepository.findAll().forEach(System.out::println);
        System.out.println("Пользователи:");
        userRepository.findAll().forEach(System.out::println);
        User customer = userRepository.findByEmail("anna@example.org").orElseThrow();
        System.out.println("Найден клиент: " + customer.getName());
        System.out.println("nobody@example.org: " + userRepository.findByEmail("nobody@example.org")
                .map(User::getName)
                .orElse("не найден"));
        System.out.println();

        Ticket ticket = ticketService.createTicket(
                "Не печатает принтер",
                "Принтер в бухгалтерии пишет \"замятие бумаги\"",
                TicketPriority.MEDIUM,
                customer.getId(),
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
