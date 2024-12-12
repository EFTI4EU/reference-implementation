import { Injectable } from "@angular/core";
import { CanActivate, Router } from "@angular/router";
import {SessionService} from "../services/session.service";
@Injectable({ providedIn: 'root' })

export class AuthGuard implements CanActivate {

  constructor(private router: Router, private sessionService: SessionService) {
  }

  canActivate(): boolean {
    if(!this.sessionService.isAuthenticated()) {
      this.router.navigate(['login']).then(() => {
        location.reload();
      });
    }
    return true;
  }
}
