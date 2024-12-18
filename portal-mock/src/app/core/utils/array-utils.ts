import { Injectable } from '@angular/core';
import { TransportMovement } from "../models/transportMovement.model";

@Injectable({
  providedIn: 'root'
})
export class ArrayUtils {

  public static dynamicSort(property: string) {
    let sortOrder = 1;
    if(property[0] === "-") {
      sortOrder = -1;
      property = property.substr(1);
    }
    return function (a: any,b: any) {
      const result = (a[property as keyof TransportMovement] < b[property as keyof TransportMovement]) ? -1
        : (a[property as keyof TransportMovement] > b[property as keyof TransportMovement]) ? 1 : 0;
      return result * sortOrder;
    }
  }
}
