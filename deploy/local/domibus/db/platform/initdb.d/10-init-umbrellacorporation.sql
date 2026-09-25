DROP SCHEMA IF EXISTS umbrellacorporation;
CREATE SCHEMA umbrellacorporation;
ALTER DATABASE umbrellacorporation CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
GRANT ALL PRIVILEGES ON umbrellacorporation.* TO 'edelivery'@'%';
FLUSH PRIVILEGES;
