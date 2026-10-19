package helpdesk;

import helpdesk.config.DatabaseConfig;
import helpdesk.exception.AccessDeniedException;
import helpdesk.exception.InvalidTicketStateException;
import helpdesk.exception.OptimisticLockException;
import helpdesk.exception.RatingException;
import helpdesk.model.Category;
import helpdesk.model.TicketRating;
import helpdesk.model.Ticket;
import helpdesk.model.TicketDetails;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.CategoryRepository;
import helpdesk.repository.ReportRepository;
import helpdesk.repository.SlaPolicyRepository;
import helpdesk.repository.StatusHistoryRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.TransactionRunner;
import helpdesk.repository.UserRepository;
import helpdesk.repository.jdbc.ConnectionProvider;
import helpdesk.repository.jdbc.JdbcAssignmentHistoryRepository;
import helpdesk.repository.jdbc.JdbcAuditLogRepository;
import helpdesk.repository.jdbc.JdbcCategoryRepository;
import helpdesk.repository.jdbc.JdbcCommentRepository;
import helpdesk.repository.jdbc.JdbcRatingRepository;
import helpdesk.repository.jdbc.JdbcReportRepository;
import helpdesk.repository.jdbc.JdbcSlaPolicyRepository;
import helpdesk.repository.jdbc.JdbcStatusHistoryRepository;
import helpdesk.repository.jdbc.JdbcTicketRepository;
import helpdesk.repository.jdbc.JdbcTransactionManager;
import helpdesk.repository.jdbc.JdbcUserRepository;
import helpdesk.service.AssignmentService;
import helpdesk.service.AuditService;
import helpdesk.service.CategoryService;
import helpdesk.service.CommentService;
import helpdesk.service.ConsoleNotificationService;
import helpdesk.service.RatingService;
import helpdesk.service.TicketService;
import helpdesk.ui.DurationFormat;
import helpdesk.ui.TicketCardPrinter;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class Main {

    private final TransactionRunner transactions;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatusHistoryRepository historyRepository;
    private final ReportRepository reportRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final TicketService ticketService;
    private final CommentService commentService;
    private final CategoryService categoryService;
    private final AssignmentService assignmentService;
    private final AuditService auditService;
    private final RatingService ratingService;

    private Main(ConnectionProvider dataSource) {
        JdbcTransactionManager connections = new JdbcTransactionManager(dataSource);
        transactions = connections;
        userRepository = new JdbcUserRepository(connections);
        categoryRepository = new JdbcCategoryRepository(connections);
        historyRepository = new JdbcStatusHistoryRepository(connections);
        reportRepository = new JdbcReportRepository(connections);
        slaPolicyRepository = new JdbcSlaPolicyRepository(connections);
        ticketRepository = new JdbcTicketRepository(connections);
        ConsoleNotificationService notifications = new ConsoleNotificationService();
        auditService = new AuditService(new JdbcAuditLogRepository(connections));
        ticketService = new TicketService(connections, ticketRepository, userRepository, categoryRepository,
                historyRepository, slaPolicyRepository, auditService, notifications);
        assignmentService = new AssignmentService(connections, ticketRepository, userRepository,
                historyRepository, new JdbcAssignmentHistoryRepository(connections), auditService, notifications);
        categoryService = new CategoryService(categoryRepository);
        commentService = new CommentService(connections, new JdbcCommentRepository(connections),
                ticketRepository, userRepository, auditService);
        ratingService = new RatingService(connections, new JdbcRatingRepository(connections),
                ticketRepository, userRepository, auditService);
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
            case "ratings" -> app.ratings();
            default -> System.out.println("Команды: scenario | show <id> | list | demo | ratings");
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
        assignmentService.assignTicket(ticket.getId(), agent.getId(), admin.getId());

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
        User customer = userRepository.findByEmail("anna@example.org").orElseThrow();
        User agent = userRepository.findByEmail("ivan.support@example.org").orElseThrow();
        User admin = userRepository.findByEmail("admin@example.org").orElseThrow();

        section("Задание 1. Поиск по email");
        System.out.println("  anna@example.org: " + userRepository.findByEmail("anna@example.org")
                .map(User::getName).orElse("не найден"));
        System.out.println("  nobody@example.org: " + userRepository.findByEmail("nobody@example.org")
                .map(User::getName).orElse("не найден"));

        section("Задания 3-4. updated_at и отмена заявки");
        Ticket first = ticketService.createTicket(
                "Нужна вторая мышь", "Для работы с двумя ПК", TicketPriority.LOW, customer.getId(), 3L);
        System.out.println("  создана:   updated_at = " + first.getUpdatedAt());
        Ticket cancelled = ticketService.cancelTicket(first.getId(), customer.getId());
        System.out.println("  NEW -> CANCELLED:         updated_at = " + cancelled.getUpdatedAt());
        Ticket second = ticketService.createTicket(
                "Не работает веб-камера", "Камера не определяется", TicketPriority.LOW, customer.getId(), 3L);
        assignmentService.assignTicket(second.getId(), agent.getId(), admin.getId());
        System.out.println("  IN_PROGRESS -> CANCELLED: "
                + ticketService.cancelTicket(second.getId(), customer.getId()).getStatus());
        Ticket closed = ticketService.findByStatus(TicketStatus.CLOSED).get(0);
        try {
            ticketService.cancelTicket(closed.getId(), admin.getId());
        } catch (InvalidTicketStateException e) {
            System.out.println("  Ошибка: " + e.getMessage());
        }

        section("Задания 6-7. Комментарии клиента и проверка роли");
        commentService.addComment(second.getId(), customer.getId(), "Камера заработала сама", false);
        commentService.addComment(second.getId(), agent.getId(), "Клиент отменил, драйвер не трогали", true);
        System.out.println("  Видно клиенту:");
        commentService.findVisibleToCustomer(second.getId()).forEach(c -> System.out.println("    " + c));
        try {
            assignmentService.assignTicket(second.getId(), customer.getId(), admin.getId());
        } catch (AccessDeniedException e) {
            System.out.println("  Ошибка: " + e.getMessage());
        }

        section("Задание 2. NEW + HIGH");
        ticketService.findByStatusAndPriority(TicketStatus.NEW, TicketPriority.HIGH)
                .forEach(t -> System.out.println("  " + t));

        section("Задание 5. Деактивация категории");
        categoryService.deactivateCategory(4);
        System.out.println("  " + categoryRepository.findById(4).orElseThrow());
        try {
            ticketService.createTicket("Нет доступа к 1С", "Не пускает в базу", null, customer.getId(), 4L);
        } catch (IllegalArgumentException e) {
            System.out.println("  Ошибка: " + e.getMessage());
        }
        categoryService.activateCategory(4);

        section("Задания 9-10. SLA и просроченные заявки");
        slaPolicyRepository.findAll().forEach(p -> System.out.println("  " + p));
        LocalDateTime moment = LocalDateTime.now().plusHours(3);
        System.out.println("  Просрочены на " + moment.withNano(0) + ":");
        ticketService.findOverdue(moment).forEach(t -> System.out.println("  " + t + " | срок " + t.getDueAt()));

        section("Задание 11. Пагинация (по 5 заявок)");
        int pages = ticketService.countPages(5);
        for (int page = 1; page <= 2; page++) {
            System.out.println("  Страница " + page + " из " + pages + ":");
            ticketService.findPage(page, 5).forEach(t -> System.out.println("    " + t));
        }

        section("Задание 12. Поиск ILIKE: \"ПРИНТЕР\"");
        ticketService.search("ПРИНТЕР").forEach(t -> System.out.println("  " + t));
        System.out.println("  Поиск \"100%\": " + ticketService.search("100%").size() + " заявок");

        section("Задание 21. Полнотекстовый поиск и ILIKE");
        for (String query : new String[]{"принтеры не печатают", "доступ -VPN", "почта"}) {
            System.out.println("  \"" + query + "\": ILIKE = " + ticketService.search(query).size()
                    + ", full-text = " + ticketService.fullTextSearch(query).size());
        }
        ticketService.fullTextSearch("принтеры не печатают").stream()
                .limit(3)
                .forEach(t -> System.out.println("    " + t));

        section("Задание 13. Переназначение и история назначений");
        User sergey = userRepository.findByEmail("sergey.net@example.org").orElseThrow();
        Ticket vpn = ticketService.createTicket(
                "Не подключается VPN", "Ошибка 809", TicketPriority.HIGH, customer.getId(), 1L);
        assignmentService.assignTicket(vpn.getId(), agent.getId(), admin.getId());
        assignmentService.assignTicket(vpn.getId(), sergey.getId(), admin.getId());
        assignmentService.history(vpn.getId()).forEach(h -> System.out.println("  " + h));

        section("Задание 15. Статистика сотрудников");
        System.out.printf("  %-16s %8s %8s %8s%n", "Сотрудник", "активные", "решённые", "закрытые");
        reportRepository.agentStatistics().forEach(a -> System.out.printf(
                "  %-16s %8d %8d %8d%n", a.agentName(), a.active(), a.resolved(), a.closed()));

        section("Задание 16. Среднее время решения");
        System.out.println("  Все закрытые заявки: " + reportRepository.averageResolutionTime()
                .map(DurationFormat::format)
                .orElse("нет данных"));
        reportRepository.averageResolutionTimeByCategory()
                .forEach((category, time) -> System.out.println("  " + category + ": " + DurationFormat.format(time)));

        section("Задание 19. Журнал аудита заявки №" + vpn.getId());
        auditService.ticketHistory(vpn.getId()).forEach(e -> System.out.println("  " + e));

        section("Задание 18. Optimistic locking");
        Ticket scanner = ticketService.createTicket(
                "Не работает сканер штрихкодов", "Касса 2", TicketPriority.MEDIUM, customer.getId(), 3L);
        Ticket copyA = ticketRepository.findById(scanner.getId()).orElseThrow();
        Ticket copyB = ticketRepository.findById(scanner.getId()).orElseThrow();
        System.out.println("  Обе копии прочитаны с version = " + copyA.getVersion());
        copyA.changeStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.update(copyA);
        System.out.println("  Первый пользователь сохранил изменения, version = " + copyA.getVersion());
        copyB.changeStatus(TicketStatus.CANCELLED);
        try {
            ticketRepository.update(copyB);
        } catch (OptimisticLockException e) {
            System.out.println("  Второй пользователь: " + e.getMessage());
        }

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

    private void ratings() {
        User customer = userRepository.findByEmail("anna@example.org").orElseThrow();
        User agent = userRepository.findByEmail("elena.support@example.org").orElseThrow();
        User admin = userRepository.findByEmail("admin@example.org").orElseThrow();

        section("Мини-проект B. Оценка качества");
        Ticket ticket = ticketService.createTicket(
                "Не запускается Teams", "Белый экран после обновления", TicketPriority.MEDIUM, customer.getId(), 2L);
        assignmentService.assignTicket(ticket.getId(), agent.getId(), admin.getId());
        try {
            ratingService.rateTicket(ticket.getId(), customer.getId(), 5, "Спасибо!");
        } catch (RatingException e) {
            System.out.println("  Ошибка: " + e.getMessage());
        }
        ticketService.changeStatus(ticket.getId(), TicketStatus.RESOLVED, agent.getId());
        ticketService.changeStatus(ticket.getId(), TicketStatus.CLOSED, agent.getId());

        TicketRating rating = ratingService.rateTicket(
                ticket.getId(), customer.getId(), 3, "Помогли, но пришлось ждать полдня");
        System.out.println("  " + rating);
        ratingService.replyToRating(rating.getId(), agent.getId(), "Извините за задержку, была очередь заявок");
        ratingService.comments(rating.getId()).forEach(c -> System.out.println("    " + c));
        try {
            ratingService.rateTicket(ticket.getId(), customer.getId(), 5, null);
        } catch (RatingException e) {
            System.out.println("  Ошибка: " + e.getMessage());
        }

        section("Средняя оценка сотрудников");
        ratingService.averageByAgent().forEach(r -> System.out.printf(
                "  %-16s %.2f (оценок: %d)%n", r.agentName(), r.averageScore(), r.ratingsCount()));

        section("Заявки с оценкой 3 и ниже");
        ratingService.findLowRated(3).forEach(r -> System.out.printf(
                "  #%d %s | %s | оценка %d | %s%n",
                r.ticketId(), r.title(), r.agentName(), r.score(), orDash(r.lastComment())));
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static String orDash(String value) {
        return value == null ? "-" : value;
    }
}
