--liquibase formatted sql
--changeset mattiuusitalo:1 splitStatements:true endDelimiter:;

CREATE TABLE consignment
(
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    gate_id text NOT NULL,
    platform_id text NOT NULL,
    dataset_id text NOT NULL,
    carrier_acceptance_datetime timestamptz, -- eFTI39
    delivery_event_actual_occurrence_datetime timestamptz, -- eFTI188

    CONSTRAINT consignment_uil_unique UNIQUE (gate_id, platform_id, dataset_id)
);

CREATE TABLE main_carriage_transport_movement (
    consignment_id bigint NOT NULL REFERENCES consignment(id),
    ordinal int NOT NULL,
    mode_code smallint NOT NULL, --eFTI581
    dangerous_goods_indicator boolean, --eFTI1451
    used_transport_means_id text CHECK (LENGTH(used_transport_means_id) <= 17), --eFTI618
    used_transport_means_registration_country text, --eFTI620
    PRIMARY KEY (consignment_id, ordinal)
);

CREATE TABLE used_transport_equipment (
    consignment_id bigint NOT NULL REFERENCES consignment(id),
    sequence_number int NOT NULL, --eFTI987
    id text CHECK(LENGTH(id) <= 17), --eFTI374
    id_scheme_agency_id text,
    registration_country text, --eFTI578
    PRIMARY KEY (consignment_id, sequence_number)
);

CREATE TABLE carried_transport_equipment (
    consignment_id bigint NOT NULL,
    transport_equipment_sequence_number int NOT NULL,
    sequence_number int NOT NULL, --eFTI1000
    equipment_id text CHECK(LENGTH(equipment_id) <= 17), --eFTI374
    id_scheme_agency_id text,
    PRIMARY KEY (consignment_id, transport_equipment_sequence_number, sequence_number),
    CONSTRAINT carried_transport_equipment_fk
        FOREIGN KEY(consignment_id, transport_equipment_sequence_number) REFERENCES used_transport_equipment(consignment_id, sequence_number)
);