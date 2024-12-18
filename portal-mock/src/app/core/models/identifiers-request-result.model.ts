import { Identifiers } from "./identifiers.model";

export interface IdentifiersRequestResultModel {
  gateIndicator: string;
  status: string;
  errorCode: string
  errorDescription: string;
  consignments: Identifiers[];
}
