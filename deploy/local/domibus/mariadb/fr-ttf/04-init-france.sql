drop schema if exists france;
create schema france;
alter database france charset=utf8mb4 collate=utf8mb4_bin;
grant all on france.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
