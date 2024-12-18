import { TransportMovement } from "./transportMovement.model";
import { UsedTransportEquipment } from "./usedTransportEquipment.model";

export interface Identifiers {
  id: number;
  gateId: string;
  datasetId: string;
  platformId: string;
  carrierAcceptanceDatetime: string;
  deliveryEventActualOccurrenceDatetime: string;
  mainCarriageTransportMovement: TransportMovement[]
  usedTransportEquipment: UsedTransportEquipment[]
}
