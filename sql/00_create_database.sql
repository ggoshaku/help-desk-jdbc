CREATE ROLE helpdesk_app
    WITH LOGIN PASSWORD :'app_password';

CREATE DATABASE helpdesk_db
    OWNER helpdesk_app
    ENCODING 'UTF8';
