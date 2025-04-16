drop schema if exists massivedynamic;
create schema massivedynamic;
alter database massivedynamic charset=utf8mb4 collate=utf8mb4_bin;
grant all on massivedynamic.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
