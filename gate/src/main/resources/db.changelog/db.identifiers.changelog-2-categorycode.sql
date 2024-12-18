--liquibase formatted sql
--changeset mattiuusitalo:2 splitStatements:true endDelimiter:;

ALTER TABLE used_transport_equipment
    ADD category_code text CHECK (LENGTH(category_code) <= 17);