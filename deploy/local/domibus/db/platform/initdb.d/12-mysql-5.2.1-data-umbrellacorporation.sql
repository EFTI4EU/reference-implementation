USE umbrellacorporation;

--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/releases/5.2.1/5.2.1-changelog-data.xml
--  Ran at: 8/11/26, 12:29 PM
--  Against: null@offline:mysql?changeLogFile=target/liquibase/changelog-1.19-data.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************

--  Changeset src/main/resources/db/releases/5.2.1/../../common/changelog-before-migration-statements-v2.xml::EDELIVERY-12287_assert_previous_migration_succeeded-v2-mysql::Gabriel Maier
DELIMITER //

DROP PROCEDURE IF EXISTS ASSERT_DB_VERSION_IS
//

DROP FUNCTION IF EXISTS STRING_TO_JSON_ARRAY
//

CREATE FUNCTION STRING_TO_JSON_ARRAY(in_string VARCHAR(100), in_delimiter VARCHAR(1))
RETURNS JSON
DETERMINISTIC
BEGIN
    RETURN CAST(CONCAT('["', REPLACE(in_string, in_delimiter, '","'), '"]') as JSON);
END
//

CREATE PROCEDURE ASSERT_DB_VERSION_IS(IN in_expected_version VARCHAR(100))
BEGIN
    DECLARE actual_version VARCHAR(30);
    DECLARE numeric_version INT;

    IF in_expected_version IS NOT NULL THEN
        SET in_expected_version = REPLACE(in_expected_version, ' ', '');
    END IF;
    IF in_expected_version = 'PreconditionDomibusVersionIs_not_set' OR in_expected_version IS NULL THEN
        SET in_expected_version = 'ignore';
    END IF;
    IF in_expected_version = 'empty' THEN
        SELECT count(1)
        INTO @table_count
        FROM information_schema.tables
        WHERE table_type = 'BASE TABLE'
          AND lower(table_schema) = lower(database());
        IF @table_count > 0 THEN
                    SET @error_message = CONCAT('Domibus Assertion Error: The schema ', database(), ' should be empty to run this script file') ;
                    SIGNAL SQLSTATE '45000'
                        SET MESSAGE_TEXT = @error_message;
        END IF;
    ELSEIF in_expected_version <> 'ignore' THEN
        SELECT V.VERSION, SUBSTRING_INDEX(V.VERSION, '.', 1) * 100 * 100 +  -- major version
                          SUBSTRING_INDEX(SUBSTRING_INDEX(V.VERSION, '.', 2), '.', -1) * 100 +  -- minor version
                          IFNULL(           -- patch version
                                  CASE
                                      WHEN V.VERSION LIKE '%.%.%'
                                          THEN CAST(SUBSTRING_INDEX(V.VERSION, '.', -1) AS UNSIGNED)
                                      END,
                                  0
                          ) as version_as_number
        INTO actual_version, numeric_version
        FROM TB_VERSION V
        ORDER BY version_as_number DESC
        LIMIT 1;
        IF NOT JSON_CONTAINS(STRING_TO_JSON_ARRAY(in_expected_version, ','), JSON_QUOTE(actual_version)) THEN
            SET @error_message = CONCAT('Domibus Assertion Error: Please upgrade to ', in_expected_version, '. The last successful upgrade was to ', actual_version);
            SIGNAL SQLSTATE '45001'
                SET MESSAGE_TEXT = @error_message;
        END IF;
    END IF;
END
//
CALL ASSERT_DB_VERSION_IS('ignore');
//

DELIMITER //

DROP PROCEDURE IF EXISTS EXECUTE_AND_IGNORE_ERROR
//

/*  Call this stored procedure to execute the statement contained in the parameter dmlStatement.
 In case the execution causes an error the statement that failed is rolled back.
 If the MYSQL_ERRNO equals errorNumber then a warning message is logged and the error is ignored otherwise the error is rethrown with the SQLSTATE code 45001 */
CREATE PROCEDURE EXECUTE_AND_IGNORE_ERROR(IN dmlStatement MEDIUMTEXT, IN errorNumber NUMERIC)
    MODIFIES SQL DATA
    SQL SECURITY DEFINER
BEGIN
    DECLARE code NUMERIC DEFAULT -1;
    DECLARE msg TEXT;

    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
        BEGIN
            GET DIAGNOSTICS CONDITION 1
                code = MYSQL_ERRNO, msg = MESSAGE_TEXT;
            IF code = errorNumber THEN
                SELECT CONCAT('Warning: Ignoring error [', code, ': ', msg, '] after executing [', dmlStatement, ']') AS Message;
            ELSE
                SET @error_message = CONCAT('Unhandled exception [', code, ': ', msg, ']') ;
                SIGNAL SQLSTATE '45001'
                    SET MESSAGE_TEXT = @error_message;
            END IF;
        END;

    SET @q = dmlStatement;
    PREPARE stmt FROM @q;
    IF code = -1 THEN
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END
//

--  Changeset src/main/resources/db/releases/5.2.1/5.2.1-changelog-data.xml::EDELIVERY-2144_1::thomas dussart
INSERT INTO TB_USER_ROLE (ID_PK, ROLE_NAME) VALUES ('197001010000000001', 'ROLE_ADMIN');

INSERT INTO TB_USER_ROLE (ID_PK, ROLE_NAME) VALUES ('197001010000000002', 'ROLE_USER');

--  Changeset src/main/resources/db/releases/5.2.1/5.2.1-changelog-data.xml::EDELIVERY-7368::ionperpegel
INSERT INTO TB_D_MSH_ROLE (ID_PK, `ROLE`) VALUES ('197001010000000001', 'SENDING');

INSERT INTO TB_D_MSH_ROLE (ID_PK, `ROLE`) VALUES ('197001010000000002', 'RECEIVING');

--  Changeset src/main/resources/db/releases/5.2.1/5.2.1-changelog-data.xml::EDELIVERY-7836-insert::idragusa
INSERT INTO TB_USER_MESSAGE (ID_PK, MSH_ROLE_ID_FK) VALUES ('19700101', '197001010000000001');

--  Changeset src/main/resources/db/releases/5.2.1/5.2.1-changelog-data.xml::EDELIVERY-8503_2::ion perpegel
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000001', 'bootstrap-synchronization.lock');

--  Changeset src/main/resources/db/releases/5.2.1/5.2.1-changelog-data.xml::EDELIVERY-9451::ion perpegel
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000002', 'scheduler-synchronization.lock');

--  Changeset src/main/resources/db/releases/5.2.1/5.2.1-changelog-data.xml::EDELIVERY-11248::ion perpegel
INSERT INTO TB_LOCK (ID_PK, LOCK_KEY) VALUES ('197001010000000003', 'keystore-synchronization.lock');

--  Changeset src/main/resources/db/releases/5.2.1/5.2.1-changelog-data.xml::insert_last_pk_in_TB_EARCHIVE_START::gautifr
INSERT INTO TB_EARCHIVE_START (ID_PK, LAST_PK_USER_MESSAGE, `DESCRIPTION`, MODIFICATION_TIME, MODIFIED_BY) VALUES ('1', '0', 'START ID_PK FOR CONTINUOUS EXPORT', (UTC_TIMESTAMP), 'DOMIBUS');

INSERT INTO TB_EARCHIVE_START (ID_PK, LAST_PK_USER_MESSAGE, `DESCRIPTION`, MODIFICATION_TIME, MODIFIED_BY) VALUES ('2', '0', 'START ID_PK FOR SANITY EXPORT', (UTC_TIMESTAMP), 'DOMIBUS');

--  Changeset src/main/resources/db/releases/5.2.1/../../common/changelog-version-inserts.xml::EDELIVERY-7668-mysql::Catalin Enache
INSERT INTO TB_VERSION (VERSION, BUILD_TIME, CREATION_TIME) VALUES ('5.2.1', '2026-08-11 12:29', (UTC_TIMESTAMP))
            ON DUPLICATE KEY UPDATE BUILD_TIME='2026-08-11 12:29', CREATION_TIME = (UTC_TIMESTAMP);
