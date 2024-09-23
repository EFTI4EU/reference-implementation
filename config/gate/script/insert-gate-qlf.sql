TRUNCATE eftifr.gate;

INSERT INTO eftifr.gate(country, url, createddate, lastmodifieddate) VALUES
 ('BO', 'https://mock.efti.qlf.k8s.groupein.local/bo', now(), now()),
 ('SY', 'https://mock.efti.qlf.k8s.groupein.local/sy', now(), now()),
 ('FR', 'https://gate-fr.efti.qlf.k8s.groupein.local', now(), now())
