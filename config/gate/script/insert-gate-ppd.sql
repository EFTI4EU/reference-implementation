/*
   this script must be played for each gate
   dont forget to update the related schema
 */

TRUNCATE eftifr.gate;

INSERT INTO eftifr.gate(country, gateid, createddate, lastmodifieddate) VALUES
    ('FR', 'france', now(), now());
