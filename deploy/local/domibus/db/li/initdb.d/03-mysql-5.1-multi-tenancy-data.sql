--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/changelog-multi-tenancy-data.xml
--  Ran at: 3/24/23 6:43 PM
--  Against: null@offline:mysql?changeLogFile=/Users/dragusa/domibus_release_504/domibus/Core/Domibus-MSH-db/target/liquibase/changelog-5.1-multi-tenancy-data.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************

USE general_schema;

--  Changeset src/main/resources/db/changelog-multi-tenancy-data.xml::EDELIVERY-4916::Catalin Enache
INSERT INTO TB_USER_ROLE (ID_PK, ROLE_NAME) VALUES ('197001010000000003', 'ROLE_AP_ADMIN');

INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000001', 'bootstrap-synchronization.lock');

--  Changeset src/main/resources/db/changelog-multi-tenancy-data.xml::EDELIVERY-9451::ion perpegel
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000002', 'scheduler-synchronization.lock');

--  Changeset src/main/resources/db/common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.1', '2023-03-24 16:41', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='2023-03-24 16:41', CREATION_TIME = (UTC_TIMESTAMP);
