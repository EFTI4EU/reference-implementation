drop schema if exists borduria;
create schema borduria;
alter database borduria charset=utf8mb4 collate=utf8mb4_bin;
grant all on borduria.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
