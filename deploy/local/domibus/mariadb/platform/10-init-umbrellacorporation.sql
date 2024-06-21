drop schema if exists umbrellacorporation;
create schema umbrellacorporation;
alter database umbrellacorporation charset=utf8mb4 collate=utf8mb4_bin;
grant all on umbrellacorporation.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
