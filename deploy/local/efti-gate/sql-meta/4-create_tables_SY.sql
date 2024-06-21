-- create schema
CREATE SCHEMA eftiSY;

-- Give permission to schema and table created
grant all privileges on schema eftiSY to root;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA eftiSY to root;
