package helpdesk.service;

import helpdesk.model.User;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final List<String> records = new ArrayList<>();

    public void record(User user, String action) {
        records.add(LocalTime.now().format(TIME_FORMAT) + " " + user.getName() + ": " + action);
    }

    public List<String> getRecords() {
        return Collections.unmodifiableList(records);
    }
}
