-- create schema
CREATE SCHEMA eftiBO;

-- Give permission to schema and table created
grant all privileges on schema eftiBO to efti;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA eftiBO TO efti;
