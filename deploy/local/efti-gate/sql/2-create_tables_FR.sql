-- create schema
CREATE SCHEMA eftiFR;

-- Give permission to schema and table created
grant all privileges on schema eftiFR to efti;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA eftiFR TO efti;
