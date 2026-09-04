drop schema if exists listenbourg;
create schema listenbourg;
alter database listenbourg charset=utf8mb4 collate=utf8mb4_bin;
grant all on listenbourg.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
