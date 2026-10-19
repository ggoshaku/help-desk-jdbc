package helpdesk;

import helpdesk.exception.InvalidTicketStateException;
import helpdesk.exception.TicketNotFoundException;
import helpdesk.model.Administrator;
import helpdesk.model.Customer;
import helpdesk.model.SupportAgent;
import helpdesk.model.Ticket;
import helpdesk.model.TicketCategory;
import helpdesk.model.TicketPriority;
import helpdesk.model.TicketStatus;
import helpdesk.model.User;
import helpdesk.repository.InMemoryTicketRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.service.AuditService;
import helpdesk.service.ConsoleNotificationService;
import helpdesk.service.EmailNotificationService;
import helpdesk.service.NotificationService;
import helpdesk.service.TelegramNotificationService;
import helpdesk.service.TicketService;

import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== HELP DESK ===");

        Customer customer = new Customer(1, "Анна Петрова", "anna@mail.ru");
        SupportAgent agent = new SupportAgent(2, "Сергей Иванов", "sergey@helpdesk.ru");
        Administrator admin = new Administrator(3, "Олег Смирнов", "admin@helpdesk.ru");

        TicketRepository repository = new InMemoryTicketRepository();
        NotificationService notificationService = new ConsoleNotificationService();
        AuditService auditService = new AuditService();
        TicketService ticketService = new TicketService(repository, notificationService, auditService);

        runLifecycle(customer, agent, ticketService);

        Ticket appTicket = new Ticket(
                2,
                "Ошибка приложения",
                "1С падает при формировании отчёта",
                TicketPriority.MEDIUM,
                TicketCategory.SOFTWARE
        );
        Ticket printerTicket = new Ticket(
                3,
                "Не печатает принтер",
                "Принтер в бухгалтерии пишет \"замятие бумаги\"",
                TicketPriority.LOW,
                TicketCategory.HARDWARE
        );
        Ticket vpnTicket = new Ticket(
                4,
                "Нет доступа к VPN",
                "Не подключается удалённый рабочий стол",
                TicketPriority.CRITICAL,
                TicketCategory.ACCOUNT
        );
        for (Ticket ticket : List.of(appTicket, printerTicket, vpnTicket)) {
            ticketService.registerTicket(ticket, customer);
        }

        printSection("Все заявки");
        printTickets(repository.findAll());

        showSearch(repository);
        showNotificationChannels(customer, agent, repository, auditService, appTicket);
        showErrors(customer, agent, ticketService, printerTicket);

        printSection("История заявки #1");
        ticketService.getTicket(1).getHistory().forEach(System.out::println);

        printSection("Статистика");
        ticketService.getStatistics()
                .forEach((status, count) -> System.out.println(status + ": " + count));

        printSection("Просроченные заявки через 3 часа");
        printTickets(ticketService.findOverdue(LocalDateTime.now().plusHours(3)));

        printSection("Заявки специалиста " + agent.getName());
        printTickets(agent.getAssignedTickets());

        printSection("Журнал действий");
        auditService.getRecords().forEach(System.out::println);

        printSection("Пользователи");
        for (User user : List.of(customer, agent, admin)) {
            user.performAction();
        }
    }

    private static void runLifecycle(Customer customer, SupportAgent agent, TicketService ticketService) {
        System.out.println("Клиент: " + customer.getName());

        Ticket ticket = new Ticket(
                1,
                "Не работает Wi-Fi",
                "Ноутбук не видит сеть в аудитории 305",
                TicketPriority.HIGH,
                TicketCategory.NETWORK
        );
        System.out.println("Заявка #" + ticket.getId() + ": " + ticket.getTitle() + " | " + ticket.getStatus());

        ticketService.registerTicket(ticket, customer);
        ticketService.startTicket(ticket, agent);
        printStatus(ticket);
        ticketService.resolveTicket(ticket, agent);
        printStatus(ticket);
        ticketService.closeTicket(ticket, agent);
        printStatus(ticket);
    }

    private static void showSearch(TicketRepository repository) {
        printSection("Поиск");
        System.out.println("Новые заявки:");
        printTickets(repository.findByStatus(TicketStatus.NEW));
        System.out.println("Заявки с приоритетом CRITICAL:");
        printTickets(repository.findByPriority(TicketPriority.CRITICAL));
        System.out.println("Категория \"" + TicketCategory.HARDWARE.getTitle() + "\":");
        printTickets(repository.findByCategory(TicketCategory.HARDWARE));
        System.out.println("Заявка #2: " + repository.findById(2).map(Ticket::getTitle).orElse("не найдена"));
        System.out.println("Заявка #42: " + repository.findById(42).map(Ticket::getTitle).orElse("не найдена"));
    }

    private static void showNotificationChannels(Customer customer, SupportAgent agent,
                                                 TicketRepository repository, AuditService auditService,
                                                 Ticket ticket) {
        printSection("Другие способы уведомлений");
        TicketService emailTicketService =
                new TicketService(repository, new EmailNotificationService(customer.getEmail()), auditService);
        TicketService telegramTicketService =
                new TicketService(repository, new TelegramNotificationService("@helpdesk_support"), auditService);

        emailTicketService.startTicket(ticket, agent);
        telegramTicketService.resolveTicket(ticket, agent);
    }

    private static void showErrors(Customer customer, SupportAgent agent,
                                   TicketService ticketService, Ticket ticket) {
        printSection("Ошибки");
        ticketService.cancelTicket(ticket, customer);
        try {
            ticketService.startTicket(ticket, agent);
        } catch (InvalidTicketStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
        try {
            ticketService.getTicket(42);
        } catch (TicketNotFoundException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
        try {
            new Ticket(5, "   ", "Без названия", TicketPriority.LOW, TicketCategory.HARDWARE);
        } catch (IllegalArgumentException e) {
            System.out.println("Заявка не создана: " + e.getMessage());
        }
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void printStatus(Ticket ticket) {
        System.out.println("Статус: " + ticket.getStatus());
    }

    private static void printTickets(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            System.out.println("(нет заявок)");
        }
        for (Ticket ticket : tickets) {
            System.out.println(ticket);
        }
    }
}
