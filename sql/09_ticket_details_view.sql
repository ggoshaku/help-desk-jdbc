CREATE VIEW ticket_details AS
SELECT
    t.id,
    t.title,
    t.status,
    t.priority,
    c.name AS category_name,
    customer.name AS customer_name,
    assignee.name AS assignee_name,
    t.created_at,
    t.updated_at,
    t.closed_at,
    t.due_at
FROM tickets t
JOIN users customer ON customer.id = t.customer_id
LEFT JOIN users assignee ON assignee.id = t.assignee_id
LEFT JOIN categories c ON c.id = t.category_id;
