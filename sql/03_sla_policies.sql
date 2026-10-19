CREATE TABLE sla_policies (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    priority         ticket_priority NOT NULL UNIQUE,
    response_minutes INTEGER NOT NULL CHECK (response_minutes > 0),
    resolve_minutes  INTEGER NOT NULL CHECK (resolve_minutes >= response_minutes)
);

INSERT INTO sla_policies(name, priority, response_minutes, resolve_minutes) VALUES
('Критичный', 'CRITICAL', 10, 30),
('Высокий', 'HIGH', 30, 120),
('Средний', 'MEDIUM', 60, 480),
('Низкий', 'LOW', 240, 1440);

ALTER TABLE tickets ADD COLUMN due_at TIMESTAMP;

UPDATE tickets t
SET due_at = t.created_at + s.resolve_minutes * INTERVAL '1 minute'
FROM sla_policies s
WHERE s.priority = t.priority;

CREATE INDEX idx_tickets_active_due_at
    ON tickets(due_at)
    WHERE status IN ('NEW', 'IN_PROGRESS');
