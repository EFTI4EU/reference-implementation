-- create schema
CREATE SCHEMA eftiSY;

-- Give permission to schema and table created
grant all privileges on schema eftiSY to efti;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA eftiSY TO efti;
