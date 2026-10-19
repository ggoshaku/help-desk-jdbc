package helpdesk.repository.memory;

import helpdesk.model.SlaPolicy;
import helpdesk.model.TicketPriority;
import helpdesk.repository.SlaPolicyRepository;

import java.util.List;
import java.util.Optional;

public class InMemorySlaPolicyRepository implements SlaPolicyRepository {
    private final List<SlaPolicy> policies = List.of(
            new SlaPolicy(1, "Критичный", TicketPriority.CRITICAL, 10, 30),
            new SlaPolicy(2, "Высокий", TicketPriority.HIGH, 30, 120),
            new SlaPolicy(3, "Средний", TicketPriority.MEDIUM, 60, 480),
            new SlaPolicy(4, "Низкий", TicketPriority.LOW, 240, 1440)
    );

    @Override
    public List<SlaPolicy> findAll() {
        return policies;
    }

    @Override
    public Optional<SlaPolicy> findByPriority(TicketPriority priority) {
        return policies.stream()
                .filter(policy -> policy.priority() == priority)
                .findFirst();
    }
}
