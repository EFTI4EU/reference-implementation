import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, Observable, of, ReplaySubject, tap } from "rxjs";
import { UserInfos } from "../models/user-infos";

@Injectable({
  providedIn: 'root'
})
export class SessionService {

  private _userInfos?: UserInfos;
  public userInfoSubject: ReplaySubject<UserInfos>;

  constructor(private httpClient: HttpClient) {
    this.userInfoSubject = new ReplaySubject(1);
  }

  loadUserInfos(): Observable<any> {
    return this.httpClient.get<any>('/redirect_uri?info=json')
      .pipe(
        tap(info => {
          this._userInfos = info.userinfo;
          this.userInfoSubject.next(this.userInfos!);
          this.userInfoSubject.complete();
        }),
        catchError(() => {
          this.userInfoSubject.error(undefined);
          return of(null);
        })
      );
  }

  get userInfos(): UserInfos | undefined {
    return this._userInfos;
  }

  isAuthenticated(): boolean {
    return !!this._userInfos;
  }

  logout(): void {
    window.open('/redirect_uri?logout=', '_self');
  }
}
