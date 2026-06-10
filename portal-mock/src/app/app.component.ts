import {Component} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {MenuComponent} from "./layout/menu/menu.component";
import {LoaderComponent} from "./layout/loader/loader.component";

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, MenuComponent, LoaderComponent],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'eFTI-Portal';
}
