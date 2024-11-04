drop schema if exists ttf;
create schema ttf;
alter database ttf charset=utf8mb4 collate=utf8mb4_bin;
grant all on ttf.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
