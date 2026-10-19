CREATE TABLE ticket_ratings (
    id          BIGSERIAL PRIMARY KEY,
    ticket_id   BIGINT NOT NULL UNIQUE REFERENCES tickets(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES users(id),
    agent_id    BIGINT NOT NULL REFERENCES users(id),
    score       SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rating_comments (
    id         BIGSERIAL PRIMARY KEY,
    rating_id  BIGINT NOT NULL REFERENCES ticket_ratings(id) ON DELETE CASCADE,
    author_id  BIGINT NOT NULL REFERENCES users(id),
    text       TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT rating_comment_not_blank
        CHECK (length(trim(text)) > 0)
);

CREATE INDEX idx_ticket_ratings_agent ON ticket_ratings(agent_id);
CREATE INDEX idx_rating_comments_rating ON rating_comments(rating_id);

INSERT INTO ticket_ratings(ticket_id, customer_id, agent_id, score, created_at)
SELECT t.id,
       t.customer_id,
       t.assignee_id,
       CASE WHEN t.title = 'Мерцает монитор' THEN 2 ELSE 5 - t.id % 2 END,
       t.closed_at + INTERVAL '1 hour'
FROM tickets t
WHERE t.status = 'CLOSED' AND t.assignee_id IS NOT NULL;

INSERT INTO rating_comments(rating_id, author_id, text, created_at)
SELECT r.id, r.customer_id, 'Монитор снова мерцает через день после ремонта', r.created_at
FROM ticket_ratings r
JOIN tickets t ON t.id = r.ticket_id
WHERE t.title = 'Мерцает монитор';
