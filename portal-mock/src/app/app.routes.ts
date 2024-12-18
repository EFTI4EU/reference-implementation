import { Routes } from '@angular/router';
import {IdentifiersSearchComponent} from "./pages/identifiers-search/identifiers-search.component";
import {AuthGuard} from "./core/guard/auth.guard";
import {PostLoginGuard} from "./core/guard/post-login.guard";
import {LoginComponent} from "./pages/login/login.component";
import {IdentifiersDisplayComponent} from "./pages/identifiers-display/identifiers-display.component";
import {UilSearchComponent} from "./pages/uil-search/uil-search.component";

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'identifiers',
    pathMatch: 'full'
  },
  {
    path: 'uil',
    component: UilSearchComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'identifiers',
    component: IdentifiersSearchComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'identifiers-display/:id',
    component: IdentifiersDisplayComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [PostLoginGuard]
  }
];
