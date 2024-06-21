-- create schema
CREATE SCHEMA eftiLI;

-- Give permission to schema and table created
grant all privileges on schema eftiLI to root;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA eftiLI to root;
