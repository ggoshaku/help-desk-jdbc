#!/bin/sh
set -e

psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" \
     -v app_password="$HELPDESK_APP_PASSWORD" \
     -f /sql/00_create_database.sql

for file in /sql/[0-9]*.sql; do
    [ "$(basename "$file")" = "00_create_database.sql" ] && continue
    psql -v ON_ERROR_STOP=1 -U helpdesk_app -d helpdesk_db -f "$file"
done
