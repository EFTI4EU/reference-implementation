import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {catchError, Observable, throwError} from 'rxjs';
import {SessionService} from "../services/session.service";
import { ToastrService} from "ngx-toastr";

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  constructor(private sessionService: SessionService, private toastr: ToastrService) {
  }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(catchError(err => {
      if (request.url.indexOf('redirect_uri') < 0) {
        if (err.status === 403 && this.sessionService.isAuthenticated()) {
          location.reload();
        } else if (err.status === 401 && this.sessionService.isAuthenticated()) {

          this.toastr.error('Your session has expired, please log again');
          this.sessionService.logout();
        }
      }
      return throwError(err);
    }));
  }
}
