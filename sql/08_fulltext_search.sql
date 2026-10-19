ALTER TABLE tickets
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            setweight(to_tsvector('russian', title), 'A')
                || setweight(to_tsvector('russian', description), 'B')
        ) STORED;

CREATE INDEX idx_tickets_search_vector
    ON tickets USING GIN (search_vector);
