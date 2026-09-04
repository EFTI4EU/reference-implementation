--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/changelog-data.xml
--  Ran at: 3/24/23 6:43 PM
--  Against: null@offline:mysql?changeLogFile=/Users/dragusa/domibus_release_504/domibus/Core/Domibus-MSH-db/target/liquibase/changelog-5.1-data.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************
USE acme;

--  Changeset src/main/resources/db/changelog-data.xml::EDELIVERY-2144_1::thomas dussart
INSERT INTO TB_USER_ROLE (ID_PK, ROLE_NAME) VALUES ('197001010000000001', 'ROLE_ADMIN');

INSERT INTO TB_USER_ROLE (ID_PK, ROLE_NAME) VALUES ('197001010000000002', 'ROLE_USER');

--  Changeset src/main/resources/db/changelog-data.xml::EDELIVERY-7368::ionperpegel
INSERT INTO TB_D_MSH_ROLE (ID_PK, `ROLE`) VALUES ('197001010000000001', 'SENDING');

INSERT INTO TB_D_MSH_ROLE (ID_PK, `ROLE`) VALUES ('197001010000000002', 'RECEIVING');

--  Changeset src/main/resources/db/changelog-data.xml::EDELIVERY-7836-insert::idragusa
INSERT INTO TB_USER_MESSAGE (ID_PK, MSH_ROLE_ID_FK) VALUES ('19700101', '197001010000000001');

--  Changeset src/main/resources/db/changelog-data.xml::EDELIVERY-8503_2::ion perpegel
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000001', 'bootstrap-synchronization.lock');

--  Changeset src/main/resources/db/changelog-data.xml::EDELIVERY-9451::ion perpegel
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000002', 'scheduler-synchronization.lock');

--  Changeset src/main/resources/db/changelog-data.xml::insert_last_pk_in_TB_EARCHIVE_START::gautifr
INSERT INTO TB_EARCHIVE_START (ID_PK, LAST_PK_USER_MESSAGE, `DESCRIPTION`) VALUES ('1', '000101000000000000', 'START ID_PK FOR CONTINUOUS EXPORT');

INSERT INTO TB_EARCHIVE_START (ID_PK, LAST_PK_USER_MESSAGE, `DESCRIPTION`) VALUES ('2', '000101000000000000', 'START ID_PK FOR SANITY EXPORT');

--  Changeset src/main/resources/db/common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.1', '2023-03-24 16:41', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='2023-03-24 16:41', CREATION_TIME = (UTC_TIMESTAMP);
