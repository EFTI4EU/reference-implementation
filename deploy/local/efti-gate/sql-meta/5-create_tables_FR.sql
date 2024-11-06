-- create user
create user root with encrypted password 'root';
grant all privileges on database efti to root;


-- create schema
CREATE SCHEMA metaFR;

-- Give permission to schema and table created
grant all privileges on schema metaFR to root;


GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA metaFR to root;
