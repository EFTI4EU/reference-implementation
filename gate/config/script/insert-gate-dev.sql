/*
   this script must be played for each gate
   dont forget to update the related schema
 */

TRUNCATE eftifr.gate;

INSERT INTO eftifr.gate(country, url, createddate, lastmodifieddate) VALUES
 ('BO', 'https://mock.efti.dev.k8s.groupein.local/bo', now(), now()),
 ('SY', 'https://mock.efti.dev.k8s.groupein.local/sy', now(), now()),
 ('FR', 'https://gate-fr.efti.dev.k8s.groupein.local', now(), now())
