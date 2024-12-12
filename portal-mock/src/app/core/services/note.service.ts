import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../environment/environment";
import {NoteRequestModel} from "../models/note-request.model";

const url = environment.apiUrl.note;

@Injectable({
  providedIn: 'root'
})
export class NoteService {

  constructor(private http: HttpClient) {
  }

  postNote(note: NoteRequestModel): Observable<string> {
    return this.http.post<string>(`${url}`, note);
  }
}
