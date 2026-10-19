package helpdesk.model;

public record LowRatedTicket(long ticketId, String title, String agentName, int score, String lastComment) {
}
