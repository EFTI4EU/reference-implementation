drop schema if exists syldavia;
create schema syldavia;
alter database syldavia charset=utf8mb4 collate=utf8mb4_bin;
grant all on syldavia.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
