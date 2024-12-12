export interface IdentifiersSearch {
  modeCode: string;
  identifier: string;
  identifierType: string[];
  registrationCountryCode: string;
  dangerousGoodsIndicator: boolean | null;
  eftiGateIndicator: string[]
}
