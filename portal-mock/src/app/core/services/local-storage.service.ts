import { Injectable } from '@angular/core';
import {Identifiers} from "../models/identifiers.model";

@Injectable({
  providedIn: 'root'
})
export class LocalStorageService {

  private autoPolling: string = "AUTO-POLLING";

  addIdentifiers(identifiers: Identifiers) {
    localStorage.setItem(identifiers.datasetId, JSON.stringify(identifiers));
  }

  getIdentifiers(id: string) : Identifiers {
    return JSON.parse(localStorage.getItem(id)!);
  }

  saveAutoPolling(value: boolean) {
    localStorage.setItem(this.autoPolling, JSON.stringify(value))
  }

  getAutoPolling() : boolean {
    return JSON.parse(localStorage.getItem(this.autoPolling)!);
  }

}
