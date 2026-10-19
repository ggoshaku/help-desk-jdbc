INSERT INTO departments(name, description) VALUES
('Первая линия', 'Приём и первичная обработка обращений'),
('Сетевые инженеры', 'Сеть, Wi-Fi, VPN, маршрутизация'),
('Системные администраторы', 'Серверы и рабочие станции');

INSERT INTO categories(name, description, default_priority) VALUES
('Сеть', 'Wi-Fi, LAN, VPN, доступ в Интернет', 'HIGH'),
('Программное обеспечение', 'Ошибки и установка ПО', 'MEDIUM'),
('Оборудование', 'ПК, принтеры и периферия', 'MEDIUM'),
('Доступы', 'Учётные записи и права', 'HIGH');

INSERT INTO users(name, email, role, department_id) VALUES
('Анна Петрова', 'anna@example.org', 'CUSTOMER', NULL),
('Иван Соколов', 'ivan.support@example.org', 'SUPPORT_AGENT', 1),
('Сергей Волков', 'sergey.net@example.org', 'SUPPORT_AGENT', 2),
('Олег Админов', 'admin@example.org', 'ADMIN', 3);

INSERT INTO tickets(title, description, status, priority, customer_id, category_id)
VALUES (
    'Не работает Wi-Fi',
    'Ноутбук подключается к сети, но Интернет недоступен',
    'NEW',
    'HIGH',
    1,
    1
)
RETURNING id, title, status, created_at;
