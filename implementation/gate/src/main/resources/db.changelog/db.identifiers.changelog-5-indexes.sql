CREATE INDEX IF NOT EXISTS carried_equipment_id_index ON carried_transport_equipment (equipment_id);
CREATE INDEX IF NOT EXISTS used_equipment_id_index ON used_transport_equipment (equipment_id);
CREATE INDEX IF NOT EXISTS means_id_index ON main_carriage_transport_movement (used_transport_means_id);
