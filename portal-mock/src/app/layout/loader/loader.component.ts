import { Component } from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-loader',
  standalone: true,
  templateUrl: './loader.component.html',
  imports: [
    TranslateModule
  ],
  styleUrls: ['./loader.component.scss']
})
export class LoaderComponent {

}
