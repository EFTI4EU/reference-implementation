DROP SCHEMA IF EXISTS listenbourg;
CREATE SCHEMA listenbourg;
ALTER DATABASE listenbourg CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
GRANT ALL PRIVILEGES ON listenbourg.* TO 'edelivery'@'%';
FLUSH PRIVILEGES;
