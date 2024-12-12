import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../environment/environment";
import { RequestIdModel } from "../models/RequestId.model";
import { IdentifiersSearch } from "../models/identifiers-search.model";
import { IdentifiersResponse } from "../models/identifiers-response.model";

const url = environment.apiUrl.identifiers;

@Injectable({
  providedIn: 'root'
})
export class IdentifiersService {

  constructor(private http: HttpClient) {
  }

  postIdentifiersControl(searchParams: IdentifiersSearch): Observable<RequestIdModel> {
    return this.http.post<IdentifiersResponse>(`${url}`, searchParams);
  }

  getIdentifiersControl(requestId: string): Observable<IdentifiersResponse> {
    return this.http.get<IdentifiersResponse>(`${url}?requestId=${requestId}`);
  }
}
