# help-desk-jdbc

Практическая работа №2: консольный Help Desk из [практической работы №1](https://github.com/ggoshaku/help-desk-oop)
хранит пользователей, заявки, категории, комментарии и историю статусов в PostgreSQL. Доступ к данным через JDBC
(pgJDBC 42.7.13), без Spring и Hibernate.

Java 17, PostgreSQL 18, Maven.

## Запуск базы

Через Docker Compose:

```
cp .env.example .env        # задать свои пароли
docker compose up -d
```

При первом запуске контейнер создаёт роль `helpdesk_app`, базу `helpdesk_db` и применяет все скрипты из `sql/` по порядку.

Без Docker то же самое делается через psql:

```
psql -U postgres -v app_password='...' -f sql/00_create_database.sql
for f in sql/0[1-9]*.sql sql/1*.sql; do psql -U helpdesk_app -d helpdesk_db -f "$f"; done
```

## Запуск приложения

Параметры подключения читаются только из переменных окружения: `DB_URL`
(по умолчанию `jdbc:postgresql://localhost:5432/helpdesk_db`), `DB_USER` (по умолчанию `helpdesk_app`) и `DB_PASSWORD`.
В IntelliJ IDEA их задают в Run Configuration → Environment variables.

```
set -a; . ./.env; set +a
./mvnw compile exec:java                          # обязательный сценарий
./mvnw compile exec:java -Dexec.args="show 26"    # карточка заявки после перезапуска
./mvnw compile exec:java -Dexec.args="list"       # все заявки и отчёты
./mvnw compile exec:java -Dexec.args="demo"       # самостоятельные задания
./mvnw compile exec:java -Dexec.args="ratings"    # мини-проект «Оценка качества»
```

## Тесты

```
./mvnw test       # unit-тесты на InMemory-репозиториях
./mvnw verify     # + интеграционные тесты с PostgreSQL (пропускаются, если не задан DB_PASSWORD)
```

## Структура

```
sql/                    скрипты создания БД, схемы и миграций по порядку
src/main/java/helpdesk
├── Main.java           команды scenario / show / list / demo / ratings
├── config              DatabaseConfig
├── exception           исключения предметной области и DataAccessException
├── model               Ticket, User и роли, Category, Department, TicketComment, ...
├── repository          интерфейсы репозиториев и TransactionRunner
│   ├── jdbc            реализации на JDBC, JdbcTransactionManager
│   └── memory          InMemory-реализации для тестов
├── service             TicketService, AssignmentService, CommentService, RatingService, ...
└── ui                  вывод карточки заявки
```

## Слои

- `repository` знает SQL и превращает строки `ResultSet` в объекты. Бизнес-правил там нет.
- `service` проверяет правила (роли, допустимые переходы, активность категории) и объединяет несколько
  репозиториев в одну транзакцию через `TransactionRunner`.
- `Ticket` сам проверяет переход статуса через `TicketStatus.canTransitionTo`.
- `JdbcTransactionManager` держит соединение транзакции в `ThreadLocal`, поэтому репозитории внутри
  `inTransaction(...)` работают на одном соединении, а `commit`/`rollback` написаны в одном месте.

## Выполненные задания

- Обязательная часть: разделы 5–37.
- Уровень 1: 1–8.
- Уровень 2: 9, 10, 11, 12, 13, 15, 16.
- Уровень 3: 18, 19, 20, 21, 22, 23, 24.
- Мини-проект: вариант B «Оценка качества» (`ticket_ratings`, `rating_comments`).

## Что проверяет база, а что Java (задание 23)

В БД продублированы правила, которые относятся к одной строке и не зависят от того, кто их нарушил:
`CHECK (length(trim(title)) >= 3)`, `closed_at` заполнен тогда и только тогда, когда статус `CLOSED`,
`closed_at` и `due_at` не раньше `created_at`, оценка от 1 до 5, одна оценка на заявку (`UNIQUE`),
внешние ключи на пользователей и категории.

В Java остались правила, которым нужен контекст: допустимые переходы статуса (зависят от предыдущего значения),
кто может менять статус и назначать заявки (зависит от роли и автора заявки), назначать можно только
`SUPPORT_AGENT`, нельзя создать заявку в неактивной категории, оценить можно только закрытую заявку.
