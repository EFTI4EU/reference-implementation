-- create schema
CREATE SCHEMA eftiLI;

-- Give permission to schema and table created
grant all privileges on schema eftiLI to ingroup;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA eftiLI TO ingroup;
