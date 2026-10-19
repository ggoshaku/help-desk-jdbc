package helpdesk.model;

public record AgentStatistics(String agentName, long active, long resolved, long closed) {
}
