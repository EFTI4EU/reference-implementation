--liquibase formatted sql
--changeset mattiuusitalo:2 runOnChange:true splitStatements:true endDelimiter:;

-- Delete all gates and insert the base gates
DELETE
FROM gate
where 1 = 1;

INSERT INTO gate (country, url, createddate, lastmodifieddate)
VALUES ('LI', 'http://efti.gate.listenbourg.eu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('BO', 'http://efti.gate.borduria.eu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('SY', 'http://efti.gate.syldavia.eu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);