ALTER TABLE tickets
    ADD CONSTRAINT closed_at_matches_status
        CHECK ((status = 'CLOSED') = (closed_at IS NOT NULL)),
    ADD CONSTRAINT closed_after_created
        CHECK (closed_at IS NULL OR closed_at >= created_at),
    ADD CONSTRAINT due_after_created
        CHECK (due_at IS NULL OR due_at >= created_at);
