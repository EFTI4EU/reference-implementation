import {Component} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {MenuComponent} from "./layout/menu/menu.component";
import {UilSearchComponent} from "./pages/uil-search/uil-search.component";
import {LoaderComponent} from "./layout/loader/loader.component";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MenuComponent, UilSearchComponent, LoaderComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'eFTI-Portal';
}
