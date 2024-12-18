import { Injectable } from "@angular/core";
import { CanActivate, Router } from "@angular/router";
import {SessionService} from "../services/session.service";
@Injectable({ providedIn: 'root' })

export class PostLoginGuard implements CanActivate {

  constructor(private router: Router, private sessionService: SessionService) {
  }

  canActivate(): boolean | Promise<boolean> {
    if(this.sessionService.isAuthenticated()) {
      return this.router.navigate(['']);
    }
    return true;
  }
}
