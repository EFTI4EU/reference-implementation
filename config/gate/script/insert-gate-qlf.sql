/*
   this script must be played for each gate
   dont forget to update the related schema
 */

TRUNCATE eftifr.gate;

INSERT INTO eftifr.gate(country, gateid, createddate, lastmodifieddate) VALUES
    ('BO', 'borduria', now(), now()),
    ('SY', 'syldavia', now(), now()),
    ('FR', 'france', now(), now())
