DELETE
FROM gate
where 1 = 1; -- to explicitly state all rows are to be deleted

INSERT INTO gate (country, gateid, createddate, lastmodifieddate)
VALUES ('LI', 'listenbourg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('BO', 'borduria', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('SY', 'syldavia', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);