drop schema if exists default_domain_schema;
create schema default_domain_schema;
alter database default_domain_schema charset=utf8mb4 collate=utf8mb4_bin;
grant all on default_domain_schema.* to edelivery;
/*grant xa_recover_admin on *.* to edelivery_user;
