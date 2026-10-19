CREATE TABLE ticket_assignment_history (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    old_assignee_id BIGINT REFERENCES users(id),
    new_assignee_id BIGINT NOT NULL REFERENCES users(id),
    changed_by_id   BIGINT NOT NULL REFERENCES users(id),
    changed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assignment_history_ticket
    ON ticket_assignment_history(ticket_id);
