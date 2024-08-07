--  *********************************************************************
--  Update Database Script
--  *********************************************************************
--  Change Log: src/main/resources/db/changelog-multi-tenancy.xml
--  Ran at: 3/24/23 6:43 PM
--  Against: null@offline:mysql?changeLogFile=/Users/dragusa/domibus_release_504/domibus/Core/Domibus-MSH-db/target/liquibase/changelog-5.1-multi-tenancy.mysql
--  Liquibase version: 4.17.0
--  *********************************************************************


USE general_schema;

--  Changeset src/main/resources/db/changelog-multi-tenancy.xml::EDELIVERY-7836::gautifr
--  create DOMIBUS_SCALABLE_SEQUENCE sequence
CREATE TABLE DOMIBUS_SCALABLE_SEQUENCE (sequence_name VARCHAR(255) NOT NULL, next_val BIGINT NULL, CONSTRAINT PK_DOMIBUS_SCALABLE_SEQUENCE PRIMARY KEY (sequence_name));

--  Changeset src/main/resources/db/changelog-quartz.xml::EDELIVERY-3425::migueti
CREATE TABLE QRTZ_BLOB_TRIGGERS (SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, BLOB_DATA BLOB NULL);

CREATE TABLE QRTZ_CALENDARS (SCHED_NAME VARCHAR(120) NOT NULL, CALENDAR_NAME VARCHAR(200) NOT NULL, CALENDAR_BLOB BLOB NOT NULL);

CREATE TABLE QRTZ_CRON_TRIGGERS (SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, CRON_EXPRESSION VARCHAR(120) NOT NULL, TIME_ZONE_ID VARCHAR(80) NULL);

CREATE TABLE QRTZ_FIRED_TRIGGERS (SCHED_NAME VARCHAR(120) NOT NULL, ENTRY_ID VARCHAR(95) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, INSTANCE_NAME VARCHAR(200) NOT NULL, FIRED_TIME BIGINT NOT NULL, SCHED_TIME BIGINT NOT NULL, PRIORITY INT NOT NULL, STATE VARCHAR(16) NOT NULL, JOB_NAME VARCHAR(200) NULL, JOB_GROUP VARCHAR(200) NULL, IS_NONCONCURRENT BIT(1) NULL, REQUESTS_RECOVERY BIT(1) NULL);

CREATE TABLE QRTZ_JOB_DETAILS (SCHED_NAME VARCHAR(120) NOT NULL, JOB_NAME VARCHAR(200) NOT NULL, JOB_GROUP VARCHAR(200) NOT NULL, `DESCRIPTION` VARCHAR(250) NULL, JOB_CLASS_NAME VARCHAR(250) NOT NULL, IS_DURABLE BIT(1) NOT NULL, IS_NONCONCURRENT BIT(1) NOT NULL, IS_UPDATE_DATA BIT(1) NOT NULL, REQUESTS_RECOVERY BIT(1) NOT NULL, JOB_DATA BLOB NULL);

CREATE TABLE QRTZ_LOCKS (SCHED_NAME VARCHAR(120) NOT NULL, LOCK_NAME VARCHAR(40) NOT NULL);

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL);

CREATE TABLE QRTZ_SCHEDULER_STATE (SCHED_NAME VARCHAR(120) NOT NULL, INSTANCE_NAME VARCHAR(200) NOT NULL, LAST_CHECKIN_TIME BIGINT NOT NULL, CHECKIN_INTERVAL BIGINT NOT NULL);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, REPEAT_COUNT BIGINT NOT NULL, REPEAT_INTERVAL BIGINT NOT NULL, TIMES_TRIGGERED BIGINT NOT NULL);

CREATE TABLE QRTZ_SIMPROP_TRIGGERS (SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, STR_PROP_1 VARCHAR(512) NULL, STR_PROP_2 VARCHAR(512) NULL, STR_PROP_3 VARCHAR(512) NULL, INT_PROP_1 INT NULL, INT_PROP_2 INT NULL, LONG_PROP_1 BIGINT NULL, LONG_PROP_2 BIGINT NULL, DEC_PROP_1 DECIMAL(13, 4) NULL, DEC_PROP_2 DECIMAL(13, 4) NULL, BOOL_PROP_1 BIT(1) NULL, BOOL_PROP_2 BIT(1) NULL);

CREATE TABLE QRTZ_TRIGGERS (SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, JOB_NAME VARCHAR(200) NOT NULL, JOB_GROUP VARCHAR(200) NOT NULL, `DESCRIPTION` VARCHAR(250) NULL, NEXT_FIRE_TIME BIGINT NULL, PREV_FIRE_TIME BIGINT NULL, PRIORITY INT NULL, TRIGGER_STATE VARCHAR(16) NOT NULL, TRIGGER_TYPE VARCHAR(8) NOT NULL, START_TIME BIGINT NOT NULL, END_TIME BIGINT NULL, CALENDAR_NAME VARCHAR(200) NULL, MISFIRE_INSTR SMALLINT NULL, JOB_DATA BLOB NULL);

ALTER TABLE QRTZ_BLOB_TRIGGERS ADD PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE QRTZ_CALENDARS ADD PRIMARY KEY (SCHED_NAME, CALENDAR_NAME);

ALTER TABLE QRTZ_CRON_TRIGGERS ADD PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE QRTZ_FIRED_TRIGGERS ADD PRIMARY KEY (SCHED_NAME, ENTRY_ID);

ALTER TABLE QRTZ_JOB_DETAILS ADD PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP);

ALTER TABLE QRTZ_LOCKS ADD PRIMARY KEY (SCHED_NAME, LOCK_NAME);

ALTER TABLE QRTZ_PAUSED_TRIGGER_GRPS ADD PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP);

ALTER TABLE QRTZ_SCHEDULER_STATE ADD PRIMARY KEY (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL);

ALTER TABLE QRTZ_SIMPLE_TRIGGERS ADD PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE QRTZ_SIMPROP_TRIGGERS ADD PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

ALTER TABLE QRTZ_TRIGGERS ADD PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);

CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, JOB_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, TRIGGER_GROUP);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, INSTANCE_NAME);

CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME, REQUESTS_RECOVERY);

CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME, CALENDAR_NAME);

CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME, JOB_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME, NEXT_FIRE_TIME);

CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);

CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);

CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_STATE);

ALTER TABLE QRTZ_BLOB_TRIGGERS ADD CONSTRAINT FK_BLOB_TRIGGERS FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE QRTZ_CRON_TRIGGERS ADD CONSTRAINT FK_CRON_TRIGGERS FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE QRTZ_SIMPLE_TRIGGERS ADD CONSTRAINT FK_SIMPLE_TRIGGERS FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE QRTZ_SIMPROP_TRIGGERS ADD CONSTRAINT FK_SIMPROP_TRIGGERS FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE QRTZ_TRIGGERS ADD CONSTRAINT FK_TRIGGERS FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP) REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP) ON UPDATE NO ACTION ON DELETE NO ACTION;

--  Changeset src/main/resources/db/changelog-multi-tenancy.xml::EDELIVERY-7822,EDELIVERY-8050::Sebastian-Ion TINCU
CREATE TABLE TB_D_TIMEZONE_OFFSET (ID_PK BIGINT AUTO_INCREMENT NOT NULL, NEXT_ATTEMPT_TIMEZONE_ID VARCHAR(255) NULL COMMENT 'Time zone ID on the application server to use when converting the next attempt for displaying it to the user', NEXT_ATTEMPT_OFFSET_SECONDS INT NULL COMMENT 'Offset in seconds of the time zone on the application server to use when converting the next attempt for displaying it to the user', CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_D_TIMEZONE_OFFSET PRIMARY KEY (ID_PK));

ALTER TABLE TB_D_TIMEZONE_OFFSET ADD CONSTRAINT UK_D_TIMEZONE_OFFSET UNIQUE (NEXT_ATTEMPT_TIMEZONE_ID, NEXT_ATTEMPT_OFFSET_SECONDS);

--  Changeset src/main/resources/db/changelog-multi-tenancy.xml::1564496480476-13::Catalin Enache
--  create tables
CREATE TABLE TB_ALERT (ID_PK BIGINT AUTO_INCREMENT NOT NULL, ALERT_TYPE VARCHAR(50) NOT NULL, ATTEMPTS_NUMBER INT NULL, MAX_ATTEMPTS_NUMBER INT NOT NULL, PROCESSED BIT(1) NULL, PROCESSED_TIME timestamp NULL, REPORTING_TIME timestamp NULL, REPORTING_TIME_FAILURE timestamp NULL, NEXT_ATTEMPT timestamp NULL, FK_TIMEZONE_OFFSET BIGINT NULL, ALERT_STATUS VARCHAR(50) NOT NULL, ALERT_LEVEL VARCHAR(20) NOT NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_ALERT PRIMARY KEY (ID_PK));

ALTER TABLE TB_ALERT ADD CONSTRAINT FK_ALERT_TZ_OFFSET FOREIGN KEY (FK_TIMEZONE_OFFSET) REFERENCES TB_D_TIMEZONE_OFFSET (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE INDEX IDX_ALERT_TZ_OFFSET ON TB_ALERT(FK_TIMEZONE_OFFSET);

CREATE INDEX IDX_ALERT_STATUS ON TB_ALERT(ALERT_STATUS);

CREATE TABLE TB_COMMAND (ID_PK BIGINT AUTO_INCREMENT NOT NULL, SERVER_NAME VARCHAR(255) NOT NULL COMMENT 'The target server name', COMMAND_NAME VARCHAR(255) NOT NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_COMMAND PRIMARY KEY (ID_PK)) COMMENT='Stores commands to be executed by different nodes in clustered environments';

CREATE TABLE TB_COMMAND_PROPERTY (PROPERTY_NAME VARCHAR(50) NOT NULL, PROPERTY_VALUE VARCHAR(255) NULL, FK_COMMAND BIGINT NOT NULL COMMENT 'Reference to the parent command', CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL) COMMENT='Stores properties/parameters of the commands to be executed by different nodes in clustered environments';

CREATE TABLE TB_EVENT (ID_PK BIGINT AUTO_INCREMENT NOT NULL, EVENT_TYPE VARCHAR(50) NOT NULL, REPORTING_TIME timestamp NULL, LAST_ALERT_DATE timestamp NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_EVENT PRIMARY KEY (ID_PK));

CREATE TABLE TB_EVENT_ALERT (FK_EVENT BIGINT NOT NULL, FK_ALERT BIGINT NOT NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_EVENT_ALERT PRIMARY KEY (FK_EVENT, FK_ALERT));

CREATE TABLE TB_EVENT_PROPERTY (ID_PK BIGINT AUTO_INCREMENT NOT NULL, PROPERTY_TYPE VARCHAR(50) NOT NULL, FK_EVENT BIGINT NOT NULL, DTYPE VARCHAR(31) NULL, STRING_VALUE VARCHAR(255) NULL, DATE_VALUE timestamp NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_EVENT_PROPERTY PRIMARY KEY (ID_PK));

CREATE TABLE TB_REV_INFO (ID BIGINT AUTO_INCREMENT NOT NULL, TIMESTAMP BIGINT NULL, REVISION_DATE timestamp NULL, USER_NAME VARCHAR(255) NULL, CONSTRAINT PK_REV_INFO PRIMARY KEY (ID));

CREATE TABLE TB_REV_CHANGES (ID_PK BIGINT AUTO_INCREMENT NOT NULL, REV BIGINT NULL, AUDIT_ORDER INT NULL, ENTITY_NAME VARCHAR(255) NULL, GROUP_NAME VARCHAR(255) NULL, ENTITY_ID VARCHAR(255) NULL, MODIFICATION_TYPE VARCHAR(255) NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_REV_CHANGES PRIMARY KEY (ID_PK));

CREATE TABLE TB_USER (ID_PK BIGINT AUTO_INCREMENT NOT NULL, USER_EMAIL VARCHAR(255) NULL, USER_ENABLED BIT(1) NOT NULL, USER_PASSWORD VARCHAR(255) NOT NULL, USER_NAME VARCHAR(255) NOT NULL, OPTLOCK INT NULL, ATTEMPT_COUNT INT DEFAULT 0 NULL, SUSPENSION_DATE timestamp NULL, USER_DELETED BIT(1) DEFAULT 0 NOT NULL, PASSWORD_CHANGE_DATE timestamp DEFAULT NOW() NOT NULL, DEFAULT_PASSWORD BIT(1) DEFAULT 0 NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_USER PRIMARY KEY (ID_PK));

CREATE TABLE TB_USER_AUD (ID_PK BIGINT NOT NULL, REV BIGINT NOT NULL, REVTYPE TINYINT NULL, USER_ENABLED BIT(1) NULL, ACTIVE_MOD BIT(1) NULL, USER_DELETED BIT(1) NULL, DELETED_MOD BIT(1) NULL, USER_EMAIL VARCHAR(255) NULL, EMAIL_MOD BIT(1) NULL, USER_PASSWORD VARCHAR(255) NULL, PASSWORD_MOD BIT(1) NULL, USER_NAME VARCHAR(255) NULL, USERNAME_MOD BIT(1) NULL, OPTLOCK INT NULL, VERSION_MOD BIT(1) NULL, ROLES_MOD BIT(1) NULL, PASSWORD_CHANGE_DATE timestamp NULL, PASSWORDCHANGEDATE_MOD BIT(1) NULL, DEFAULT_PASSWORD BIT(1) NULL, DEFAULTPASSWORD_MOD BIT(1) NULL, CONSTRAINT PK_USER_AUD PRIMARY KEY (ID_PK, REV));

CREATE TABLE TB_USER_DOMAIN (ID_PK BIGINT AUTO_INCREMENT NOT NULL, USER_NAME VARCHAR(255) NULL, DOMAIN VARCHAR(255) NULL, PREFERRED_DOMAIN VARCHAR(255) NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_USER_DOMAIN PRIMARY KEY (ID_PK));

CREATE TABLE TB_USER_PASSWORD_HISTORY (ID_PK BIGINT AUTO_INCREMENT NOT NULL, USER_ID BIGINT NOT NULL, USER_PASSWORD VARCHAR(255) NOT NULL, PASSWORD_CHANGE_DATE timestamp NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_USER_PASSWORD_HISTORY PRIMARY KEY (ID_PK));

CREATE TABLE TB_USER_ROLE (ID_PK BIGINT AUTO_INCREMENT NOT NULL, ROLE_NAME VARCHAR(255) NOT NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_USER_ROLE PRIMARY KEY (ID_PK));

CREATE TABLE TB_USER_ROLES (USER_ID BIGINT NOT NULL, ROLE_ID BIGINT NOT NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_PRIMARY PRIMARY KEY (USER_ID, ROLE_ID));

CREATE TABLE TB_USER_ROLES_AUD (REV BIGINT NULL, REVTYPE TINYINT NULL, USER_ID BIGINT NULL, ROLE_ID BIGINT NULL);

CREATE TABLE TB_USER_ROLE_AUD (ID_PK BIGINT NOT NULL, REV BIGINT NOT NULL, REVTYPE TINYINT NULL, ROLE_NAME VARCHAR(255) NULL, NAME_MOD BIT(1) NULL, USERS_MOD BIT(1) NULL, CONSTRAINT PK_USER_ROLE_AUD PRIMARY KEY (ID_PK, REV));

CREATE TABLE TB_VERSION (VERSION VARCHAR(30) NULL, BUILD_TIME VARCHAR(30) NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL) COMMENT='Stores Domibus version and build time';

CREATE TABLE TB_LOCK (ID_PK BIGINT AUTO_INCREMENT NOT NULL, LOCK_KEY VARCHAR(255) NOT NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_LOCK PRIMARY KEY (ID_PK)) COMMENT='Stores keys used for locking/synchronizing in cluster';

--  Changeset src/main/resources/db/changelog-multi-tenancy.xml::EDELIVERY-9028-Audit Table for TB_USER_DOMAIN::Ion Perpegel
CREATE TABLE TB_USER_DOMAIN_AUD (ID_PK BIGINT NOT NULL, REV BIGINT NOT NULL, REVTYPE TINYINT NULL, USER_NAME VARCHAR(255) NULL, USERNAME_MOD BIT(1) NULL, DOMAIN VARCHAR(255) NULL, DOMAIN_MOD BIT(1) NULL, PREFERRED_DOMAIN VARCHAR(255) NULL, PREFERREDDOMAIN_MOD BIT(1) NULL, CONSTRAINT PK_USER_DOMAIN_AUD PRIMARY KEY (ID_PK, REV));

--  Changeset src/main/resources/db/changelog-multi-tenancy.xml::EDELIVERY-8688-General Schema Audit::Ion Perpegel
CREATE OR REPLACE VIEW V_AUDIT_DETAIL AS SELECT
            DISTINCT rc.GROUP_NAME as AUDIT_TYPE ,
            rc.MODIFICATION_TYPE as ACTION_TYPE,
            ri.USER_NAME as USER_NAME ,
            ri.REVISION_DATE as AUDIT_DATE,
            COALESCE(TRIM(CAST(rc.ENTITY_ID AS CHAR(255))), '') AS ID,
            COALESCE(TRIM(CAST(ri.ID AS CHAR(19))), '') AS REV_ID
            FROM TB_REV_INFO ri, TB_REV_CHANGES rc
            WHERE ri.ID=rc.REV;

CREATE OR REPLACE VIEW V_AUDIT AS SELECT *
            FROM V_AUDIT_DETAIL VAD
            ORDER BY VAD.AUDIT_DATE DESC;

--  Changeset src/main/resources/db/changelog-multi-tenancy.xml::1564496480476-27::Catalin Enache
--  unique constraints
ALTER TABLE TB_COMMAND_PROPERTY ADD CONSTRAINT UK_COMMAND_PROP_NAME UNIQUE (FK_COMMAND, PROPERTY_NAME);

ALTER TABLE TB_USER_ROLE ADD CONSTRAINT UK_ROLE_NAME UNIQUE (ROLE_NAME);

ALTER TABLE TB_USER_DOMAIN ADD CONSTRAINT UK_DOMAIN_USER_NAME UNIQUE (USER_NAME);

ALTER TABLE TB_USER ADD CONSTRAINT UK_USER_NAME UNIQUE (USER_NAME);

ALTER TABLE TB_LOCK ADD CONSTRAINT UK_LOCK_KEY UNIQUE (LOCK_KEY);

ALTER TABLE TB_VERSION ADD CONSTRAINT UK_VERSION UNIQUE (VERSION);

--  Changeset src/main/resources/db/changelog-multi-tenancy.xml::1564496480476-30::Catalin Enache
--  create indexes
CREATE INDEX IDX_FK_ALERT ON TB_EVENT_ALERT(FK_ALERT);

CREATE INDEX IDX_FK_EVENT_PROPERTY ON TB_EVENT_PROPERTY(FK_EVENT);

CREATE INDEX IDX_ROLE_ID ON TB_USER_ROLES(ROLE_ID);

CREATE INDEX IDX_UPH_USER_ID ON TB_USER_PASSWORD_HISTORY(USER_ID);

CREATE INDEX IDX_FK_REV_CHANGES_REV_INFO ON TB_REV_CHANGES(REV);

CREATE INDEX IDX_FK_USER_AUD_REV ON TB_USER_AUD(REV);

CREATE INDEX IDX_FK_USR_ROL_AUD_REV_INFO ON TB_USER_ROLE_AUD(REV);

CREATE INDEX IDX_FK_USR_DOM_AUD_REV_INFO ON TB_USER_DOMAIN_AUD(REV);

--  Changeset src/main/resources/db/changelog-multi-tenancy.xml::1564496480476-56::Catalin Enache
--  create foreign keys
ALTER TABLE TB_EVENT_ALERT ADD CONSTRAINT FK_ALERT_ID FOREIGN KEY (FK_ALERT) REFERENCES TB_ALERT (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_COMMAND_PROPERTY ADD CONSTRAINT FK_COMMAND_PROPERTY_ID FOREIGN KEY (FK_COMMAND) REFERENCES TB_COMMAND (ID_PK) ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE TB_EVENT_ALERT ADD CONSTRAINT FK_EVENT_ID FOREIGN KEY (FK_EVENT) REFERENCES TB_EVENT (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_EVENT_PROPERTY ADD CONSTRAINT FK_EVENT_PROPERTY_ID FOREIGN KEY (FK_EVENT) REFERENCES TB_EVENT (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_USER_PASSWORD_HISTORY ADD CONSTRAINT FK_USER_PASSWORD_HISTORY FOREIGN KEY (USER_ID) REFERENCES TB_USER (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_USER_ROLES ADD CONSTRAINT FK_USER_ROLES_ROLE FOREIGN KEY (USER_ID) REFERENCES TB_USER (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_USER_ROLES ADD CONSTRAINT FK_USER_ROLES_USER FOREIGN KEY (ROLE_ID) REFERENCES TB_USER_ROLE (ID_PK) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_REV_CHANGES ADD CONSTRAINT FK_REV_CHANGES_REV_INFO FOREIGN KEY (REV) REFERENCES TB_REV_INFO (ID) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_USER_AUD ADD CONSTRAINT FK_USER_AUD_REV FOREIGN KEY (REV) REFERENCES TB_REV_INFO (ID) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_USER_ROLE_AUD ADD CONSTRAINT FK_USR_ROL_AUD_REV_INFO FOREIGN KEY (REV) REFERENCES TB_REV_INFO (ID) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE TB_USER_DOMAIN_AUD ADD CONSTRAINT FK_USR_DOM_AUD_REV_INFO FOREIGN KEY (REV) REFERENCES TB_REV_INFO (ID) ON UPDATE RESTRICT ON DELETE RESTRICT;

--  Changeset src/main/resources/db/changelog-multi-tenancy.xml::EDELIVERY-9563::Razvan Cretu
CREATE TABLE TB_PARTY_STATUS (ID_PK BIGINT AUTO_INCREMENT NOT NULL, PARTY_NAME VARCHAR(100) NOT NULL, CONNECTIVITY_STATUS VARCHAR(50) NOT NULL, CREATION_TIME timestamp DEFAULT NOW() NOT NULL, CREATED_BY VARCHAR(255) DEFAULT 'DOMIBUS' NOT NULL, MODIFICATION_TIME timestamp NULL, MODIFIED_BY VARCHAR(255) NULL, CONSTRAINT PK_PARTY_STATUS PRIMARY KEY (ID_PK));

ALTER TABLE TB_PARTY_STATUS ADD CONSTRAINT UK_PARTY_NAME UNIQUE (PARTY_NAME);
