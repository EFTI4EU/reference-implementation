drop schema if exists general_schema;
create schema general_schema;
alter database general_schema charset=utf8mb4 collate=utf8mb4_bin;
create user 'edelivery'@'%' identified by 'edelivery';
grant all on general_schema.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
