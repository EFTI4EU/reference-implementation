--liquibase formatted sql
--changeset ousmanerabiou:3 splitStatements:true endDelimiter:;

ALTER TABLE main_carriage_transport_movement
    ADD id_scheme_agency_id text;