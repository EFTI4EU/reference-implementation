--liquibase formatted sql
--changeset mattiuusitalo:1 splitStatements:true endDelimiter:;

CREATE TABLE contactinformation
(
    id             SERIAL PRIMARY KEY NOT NULL,
    email          VARCHAR(255),
    streetname     VARCHAR(300),
    buildingnumber VARCHAR(50),
    city           VARCHAR(255),
    additionalline VARCHAR(300),
    postalcode     VARCHAR(50)
);

CREATE TABLE authority
(
    id                       SERIAL PRIMARY KEY NOT NULL,
    country                  VARCHAR(2),
    legalcontact             INTEGER,
    workingcontact           INTEGER,
    isemergencyservice       BOOLEAN,
    name                     VARCHAR(100),
    nationaluniqueidentifier VARCHAR(255),
    CONSTRAINT authority_legalcontact_fkey FOREIGN KEY (legalcontact) REFERENCES contactinformation (id) ON DELETE CASCADE,
    CONSTRAINT authority_workingcontact_fkey FOREIGN KEY (workingcontact) REFERENCES contactinformation (id) ON DELETE CASCADE
);

CREATE TABLE error
(
    id               SERIAL PRIMARY KEY,
    errorcode        VARCHAR(255),
    errordescription VARCHAR(255)
);

CREATE TABLE control
(
    id                   SERIAL PRIMARY KEY          NOT NULL,
    eftidatauuid         VARCHAR(36),
    requestuuid          VARCHAR(36)                 NOT NULL,
    requesttype          VARCHAR(55)                 NOT NULL,
    status               VARCHAR(20)                 NOT NULL,
    eftiplatformurl      VARCHAR(255),
    eftigateurl          VARCHAR(255),
    subseteurequested    VARCHAR(225)                NOT NULL,
    subsetmsrequested    VARCHAR(255)                NOT NULL,
    createddate          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    lastmodifieddate     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    transportidentifiers JSONB,
    fromgateurl          VARCHAR(255),
    authority            INTEGER,
    error                INTEGER,
    CONSTRAINT control_authority_fkey FOREIGN KEY (authority) REFERENCES authority (id) ON DELETE CASCADE,
    CONSTRAINT control_error_fkey FOREIGN KEY (error) REFERENCES error (id) ON DELETE CASCADE
);

CREATE TABLE request
(
    id                 SERIAL PRIMARY KEY          NOT NULL,
    control            INTEGER                     NOT NULL,
    status             VARCHAR(30)                 NOT NULL,
    edeliverymessageid VARCHAR(72),
    retry              INTEGER DEFAULT 0,
    reponsedata        BYTEA,
    nextretrydate      TIMESTAMP WITHOUT TIME ZONE,
    createddate        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    lastmodifieddate   TIMESTAMP WITHOUT TIME ZONE,
    gateurldest        VARCHAR(255),
    error              INTEGER,
    identifiers        JSONB,
    note               VARCHAR(255),
    request_type       VARCHAR(255),
    CONSTRAINT request_control_fkey FOREIGN KEY (control) REFERENCES control (id) ON DELETE CASCADE,
    CONSTRAINT request_error_fkey FOREIGN KEY (error) REFERENCES error (id) ON DELETE CASCADE
);

CREATE TABLE shedlock
(
    name       VARCHAR(64) PRIMARY KEY     NOT NULL,
    lock_until TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    locked_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    locked_by  VARCHAR(255)                NOT NULL
);

CREATE TABLE gate
(
    id               SERIAL PRIMARY KEY          NOT NULL,
    country          VARCHAR(10),
    url              VARCHAR(255),
    createddate      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    lastmodifieddate TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

INSERT INTO gate (id, country, url, createddate, lastmodifieddate)
VALUES (1, 'LI', 'http://efti.gate.listenbourg.eu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (2, 'BO', 'http://efti.gate.borduria.eu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (3, 'SY', 'http://efti.gate.syldavia.eu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);