-- Creates the Domibus multi-tenancy general schema and the shared database user.
-- Schema names deliberately match the ones referenced from the mounted Domibus
-- properties files (general_schema / syldavia / borduria / default_schema).
CREATE USER IF NOT EXISTS 'edelivery'@'%' IDENTIFIED BY 'edelivery';

DROP SCHEMA IF EXISTS general_schema;
CREATE SCHEMA general_schema;
ALTER DATABASE general_schema CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
GRANT ALL PRIVILEGES ON general_schema.* TO 'edelivery'@'%';
GRANT XA_RECOVER_ADMIN ON *.* TO 'edelivery'@'%';
FLUSH PRIVILEGES;
