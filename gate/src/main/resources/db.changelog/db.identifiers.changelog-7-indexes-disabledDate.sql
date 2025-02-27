CREATE INDEX IF NOT EXISTS disabled_date_index ON consignment (disabled_date);
CREATE INDEX IF NOT EXISTS consignment_id_index ON main_carriage_transport_movement (consignment_id);
