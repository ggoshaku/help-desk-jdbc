package helpdesk.repository.memory;

import helpdesk.model.User;
import helpdesk.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public User save(User user) {
        user.markSaved(nextId++, LocalDateTime.now());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String normalized = email.strip().toLowerCase();
        return users.values().stream()
                .filter(user -> user.getEmail().equals(normalized))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
}
