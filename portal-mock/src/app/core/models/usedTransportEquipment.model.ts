import { CarriedTransportEquipment } from "./carriedTransportEquipment.model";

export interface UsedTransportEquipment {
  id: number;
  sequenceNumber: number;
  schemeAgencyId: string;
  registrationCountryCode: string;
  categoryCode: string;
  carriedTransportEquipment: CarriedTransportEquipment[];
}
