package helpdesk;

import helpdesk.config.DatabaseConfig;
import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.InvalidTicketStateException;
import helpdesk.model.Ticket;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.CategoryRepository;
import helpdesk.repository.CommentRepository;
import helpdesk.repository.ReportRepository;
import helpdesk.repository.StatusHistoryRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;
import helpdesk.repository.jdbc.JdbcCategoryRepository;
import helpdesk.repository.jdbc.JdbcCommentRepository;
import helpdesk.repository.jdbc.JdbcReportRepository;
import helpdesk.repository.jdbc.JdbcStatusHistoryRepository;
import helpdesk.repository.jdbc.JdbcTicketRepository;
import helpdesk.repository.jdbc.JdbcUserRepository;
import helpdesk.service.CommentService;
import helpdesk.service.ConsoleNotificationService;
import helpdesk.service.TicketService;
import helpdesk.ui.TicketCardPrinter;

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
        CommentRepository commentRepository = new JdbcCommentRepository(DatabaseConfig::getConnection);
        ReportRepository reportRepository = new JdbcReportRepository(DatabaseConfig::getConnection);
        TicketService ticketService =
                new TicketService(ticketRepository, userRepository, new ConsoleNotificationService());
        CommentService commentService = new CommentService(commentRepository, ticketRepository, userRepository);

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
        commentService.addComment(ticket.getId(), customer.getId(), "Принтер стоит в кабинете 214", false);
        ticket = ticketService.changeStatus(ticket.getId(), TicketStatus.RESOLVED, agent.getId());
        commentService.addComment(ticket.getId(), agent.getId(), "Заменил ролик подачи бумаги", true);
        try {
            commentService.addComment(ticket.getId(), customer.getId(), "Секрет", true);
        } catch (AccessDeniedException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
        System.out.println("Все комментарии:");
        commentService.findAll(ticket.getId()).forEach(System.out::println);
        System.out.println("Комментарии, видимые клиенту:");
        commentService.findVisibleToCustomer(ticket.getId()).forEach(System.out::println);
        printTicket(ticket);

        System.out.println("История статусов:");
        historyRepository.findByTicketId(ticket.getId()).forEach(System.out::println);

        try {
            ticketService.changeStatus(ticket.getId(), TicketStatus.CLOSED, customer.getId());
        } catch (AccessDeniedException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        System.out.println(TicketCardPrinter.format(reportRepository.findDetails(ticket.getId()).orElseThrow()));

        Ticket mistake = ticketService.createTicket(
                "Ошибочная заявка", "Создана по ошибке", TicketPriority.LOW, customer.getId(), null
        );
        printTicket(ticketService.cancelTicket(mistake.getId(), customer.getId()));
        try {
            ticketService.cancelTicket(mistake.getId(), customer.getId());
        } catch (InvalidTicketStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Заявки с именами клиента и исполнителя (JOIN):");
        reportRepository.findAllDetails().forEach(d -> System.out.printf(
                "#%d %s | %s | клиент: %s | исполнитель: %s | категория: %s%n",
                d.id(), d.title(), d.status(), d.customerName(), d.assigneeName(), d.categoryName()));
        System.out.println("По статусам: " + reportRepository.countByStatus());
        System.out.println("По категориям: " + reportRepository.countByCategory());
        System.out.println("Нагрузка сотрудников: " + reportRepository.agentWorkload());
    }

    private static void printTicket(Ticket ticket) {
        System.out.println(ticket + " | updated_at = " + ticket.getUpdatedAt());
    }
}
