# help-desk-jdbc

Практическая работа №2: Help Desk из практической работы №1 переводится с хранения в памяти на PostgreSQL через JDBC.

## Запуск PostgreSQL в Docker

```
cp .env.example .env        # задать свои пароли
docker compose up -d
```

При первом запуске контейнер создаёт роль `helpdesk_app`, базу `helpdesk_db` и применяет все скрипты из `sql/` по порядку.
Приложение берёт параметры подключения только из переменных окружения `DB_URL`, `DB_USER`, `DB_PASSWORD`:

```
set -a; . ./.env; set +a
./mvnw compile exec:java
```
