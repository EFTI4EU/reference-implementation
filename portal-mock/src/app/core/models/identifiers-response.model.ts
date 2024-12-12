import {IdentifiersRequestResultModel} from "./identifiers-request-result.model";

export interface IdentifiersResponse {
  eFTIGate: string;
  requestId: string;
  status: string;
  errorCode: string
  errorDescription: string;
  identifiers: IdentifiersRequestResultModel[];
}
