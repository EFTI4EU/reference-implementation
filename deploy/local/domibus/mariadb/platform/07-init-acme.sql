drop schema if exists acme;
create schema acme;
alter database acme charset=utf8mb4 collate=utf8mb4_bin;
grant all on acme.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
