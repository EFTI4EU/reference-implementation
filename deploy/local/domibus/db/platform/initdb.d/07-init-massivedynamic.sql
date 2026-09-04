DROP SCHEMA IF EXISTS massivedynamic;
CREATE SCHEMA massivedynamic;
ALTER DATABASE massivedynamic CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
GRANT ALL PRIVILEGES ON massivedynamic.* TO 'edelivery'@'%';
FLUSH PRIVILEGES;
