DROP SCHEMA IF EXISTS default_schema;
CREATE SCHEMA default_schema;
ALTER DATABASE default_schema CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
GRANT ALL PRIVILEGES ON default_schema.* TO 'edelivery'@'%';
FLUSH PRIVILEGES;
