CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id),
    action      VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id   BIGINT NOT NULL,
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_entity
    ON audit_log(entity_type, entity_id);
