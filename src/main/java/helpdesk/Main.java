package helpdesk;

import helpdesk.config.DatabaseConfig;
import helpdesk.model.Category;
import helpdesk.model.Ticket;
import helpdesk.model.TicketDetails;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.CategoryRepository;
import helpdesk.repository.ReportRepository;
import helpdesk.repository.StatusHistoryRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.TransactionRunner;
import helpdesk.repository.UserRepository;
import helpdesk.repository.jdbc.ConnectionProvider;
import helpdesk.repository.jdbc.JdbcCategoryRepository;
import helpdesk.repository.jdbc.JdbcCommentRepository;
import helpdesk.repository.jdbc.JdbcReportRepository;
import helpdesk.repository.jdbc.JdbcStatusHistoryRepository;
import helpdesk.repository.jdbc.JdbcTicketRepository;
import helpdesk.repository.jdbc.JdbcTransactionManager;
import helpdesk.repository.jdbc.JdbcUserRepository;
import helpdesk.service.CategoryService;
import helpdesk.service.CommentService;
import helpdesk.service.ConsoleNotificationService;
import helpdesk.service.TicketService;
import helpdesk.ui.TicketCardPrinter;

import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    private final TransactionRunner transactions;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatusHistoryRepository historyRepository;
    private final ReportRepository reportRepository;
    private final TicketService ticketService;
    private final CommentService commentService;
    private final CategoryService categoryService;

    private Main(ConnectionProvider dataSource) {
        JdbcTransactionManager connections = new JdbcTransactionManager(dataSource);
        transactions = connections;
        userRepository = new JdbcUserRepository(connections);
        categoryRepository = new JdbcCategoryRepository(connections);
        historyRepository = new JdbcStatusHistoryRepository(connections);
        reportRepository = new JdbcReportRepository(connections);
        ticketRepository = new JdbcTicketRepository(connections);
        ticketService = new TicketService(connections, ticketRepository, userRepository, categoryRepository,
                historyRepository, new ConsoleNotificationService());
        categoryService = new CategoryService(categoryRepository);
        commentService = new CommentService(new JdbcCommentRepository(connections), ticketRepository, userRepository);
    }

    public static void main(String[] args) {
        System.out.println("=== HELP DESK ===");
        System.out.println("1. Подключение к PostgreSQL");
        try (Connection connection = DatabaseConfig.getConnection()) {
            System.out.println("   " + connection.getMetaData().getDatabaseProductName() + " "
                    + connection.getMetaData().getDatabaseProductVersion() + ", AutoCommit = "
                    + connection.getAutoCommit());
        } catch (SQLException e) {
            System.out.println("Не удалось подключиться к PostgreSQL: " + e.getMessage());
            return;
        }

        Main app = new Main(DatabaseConfig::getConnection);
        String command = args.length == 0 ? "scenario" : args[0];
        switch (command) {
            case "scenario" -> app.runScenario();
            case "show" -> app.show(Long.parseLong(args[1]));
            case "list" -> app.list();
            case "demo" -> app.demo();
            default -> System.out.println("Команды: scenario | show <id> | list | demo");
        }
    }

    private void runScenario() {
        System.out.println("2. Поиск клиента по email");
        User customer = userRepository.findByEmail("anna@example.org").orElseThrow();
        User agent = userRepository.findByEmail("ivan.support@example.org").orElseThrow();
        User admin = userRepository.findByEmail("admin@example.org").orElseThrow();
        System.out.println("   " + customer);

        System.out.println("3. Категории");
        for (Category category : categoryRepository.findAll()) {
            System.out.println("   " + category);
        }

        System.out.println("4. Создание заявки от имени клиента");
        Ticket ticket = ticketService.createTicket(
                "Не работает Wi-Fi в аудитории 305",
                "Ноутбук видит сеть, но не получает IP-адрес",
                TicketPriority.HIGH,
                customer.getId(),
                1L
        );

        System.out.println("5. id из PostgreSQL: " + ticket.getId());

        System.out.println("6. findById: " + ticketService.getTicket(ticket.getId()));

        System.out.println("7. Назначение сотрудника поддержки");
        ticketService.assignTicket(ticket.getId(), agent.getId(), admin.getId());

        System.out.println("8. Комментарий клиента");
        System.out.println("   " + commentService.addComment(
                ticket.getId(), customer.getId(), "Проблема только с ноутбуком, телефон подключается", false));

        System.out.println("9. IN_PROGRESS -> RESOLVED");
        ticketService.changeStatus(ticket.getId(), TicketStatus.RESOLVED, agent.getId());

        System.out.println("10. Внутренний комментарий сотрудника");
        System.out.println("   " + commentService.addComment(
                ticket.getId(), agent.getId(), "Сбросил DHCP-аренду на точке доступа", true));

        System.out.println("11. RESOLVED -> CLOSED");
        ticketService.changeStatus(ticket.getId(), TicketStatus.CLOSED, agent.getId());

        System.out.println("12. История статусов");
        historyRepository.findByTicketId(ticket.getId()).forEach(h -> System.out.println("   " + h));

        System.out.println("13. JOIN: клиент, исполнитель, категория");
        TicketDetails details = reportRepository.findDetails(ticket.getId()).orElseThrow();
        System.out.printf("   #%d %s | %s | клиент: %s | исполнитель: %s | категория: %s%n",
                details.id(), details.title(), details.status(),
                details.customerName(), details.assigneeName(), details.categoryName());

        System.out.println("14. Для проверки перезапустите программу: show " + ticket.getId());
    }

    private void show(long ticketId) {
        System.out.println("Заявка №" + ticketId + " из базы данных:");
        TicketDetails details = reportRepository.findDetails(ticketId).orElse(null);
        if (details == null) {
            System.out.println("Заявка не найдена");
            return;
        }
        System.out.println(TicketCardPrinter.format(details));
        System.out.println("История статусов:");
        historyRepository.findByTicketId(ticketId).forEach(h -> System.out.println("  " + h));
        System.out.println("Комментарии:");
        commentService.findAll(ticketId).forEach(c -> System.out.println("  " + c));
    }

    private void list() {
        for (TicketDetails d : reportRepository.findAllDetails()) {
            System.out.printf("#%d %s | %s | %s | клиент: %s | исполнитель: %s | категория: %s%n",
                    d.id(), d.title(), d.status(), d.priority(),
                    d.customerName(), orDash(d.assigneeName()), orDash(d.categoryName()));
        }
        System.out.println("По статусам: " + reportRepository.countByStatus());
        System.out.println("По категориям: " + reportRepository.countByCategory());
        System.out.println("Нагрузка сотрудников: " + reportRepository.agentWorkload());
    }

    private void demo() {
        section("Задание 2. NEW + HIGH");
        ticketService.findByStatusAndPriority(TicketStatus.NEW, TicketPriority.HIGH)
                .forEach(t -> System.out.println("  " + t));

        section("Задание 5. Деактивация категории");
        User customer = userRepository.findByEmail("anna@example.org").orElseThrow();
        categoryService.deactivateCategory(4);
        System.out.println("  " + categoryRepository.findById(4).orElseThrow());
        try {
            ticketService.createTicket("Нет доступа к 1С", "Не пускает в базу", null, customer.getId(), 4L);
        } catch (IllegalArgumentException e) {
            System.out.println("  Ошибка: " + e.getMessage());
        }
        categoryService.activateCategory(4);

        section("Rollback: сбой между UPDATE status и INSERT history");
        Ticket ticket = ticketService.createTicket(
                "Проверка отката", "Демонстрация rollback", TicketPriority.LOW, customer.getId(), 2L);
        try {
            transactions.runInTransaction(() -> {
                Ticket locked = ticketRepository.findByIdForUpdate(ticket.getId()).orElseThrow();
                locked.changeStatus(TicketStatus.CANCELLED);
                ticketRepository.update(locked);
                System.out.println("  UPDATE выполнен, статус в транзакции: " + locked.getStatus());
                throw new IllegalStateException("сбой перед записью истории");
            });
        } catch (IllegalStateException e) {
            System.out.println("  Ошибка: " + e.getMessage());
        }
        System.out.println("  Статус после rollback: " + ticketService.getTicket(ticket.getId()).getStatus());
        System.out.println("  Записей истории: " + historyRepository.findByTicketId(ticket.getId()).size());
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static String orDash(String value) {
        return value == null ? "-" : value;
    }
}
