DELETE
FROM gate
where 1 = 1; -- to explicitly state all rows are to be deleted

INSERT INTO gate (country, url, createddate, lastmodifieddate)
VALUES ('LI', 'http://efti.gate.listenbourg.eu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('BO', 'http://efti.gate.borduria.eu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('SY', 'http://efti.gate.syldavia.eu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);