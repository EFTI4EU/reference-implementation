import {Component, OnInit } from '@angular/core';
import {ActivatedRoute, Router, RouterOutlet} from "@angular/router";
import {DatePipe, NgIf, NgFor} from "@angular/common";
import { TranslateModule } from "@ngx-translate/core";
import { BootstrapIconsModule } from "ng-bootstrap-icons";
import { IconsModule } from "../../icons/icons.module";
import { ArrayUtils } from "../../core/utils/array-utils";
import { Identifiers } from "../../core/models/identifiers.model";
import { LocalStorageService } from "../../core/services/local-storage.service";
import {NgbAccordionModule, NgbCollapse} from "@ng-bootstrap/ng-bootstrap";
import {transportMode} from "../../core/models/transport-mode.model";

@Component({
  selector: 'app-identifiers-display',
  standalone: true,
  templateUrl: './identifiers-display.component.html',
  imports: [
    RouterOutlet, NgFor, NgIf, TranslateModule, BootstrapIconsModule, IconsModule,
    NgbAccordionModule, DatePipe, NgbCollapse
  ],
  styleUrl: './identifiers-display.component.css'
})
export class IdentifiersDisplayComponent implements OnInit {

  identifiers!: Identifiers;
  currentSort!: string;

  constructor(private route: ActivatedRoute, private localStorageService: LocalStorageService,
              private router: Router) {
  }

  ngOnInit() {
    this.route.params.subscribe( params => {
      this.identifiers = this.localStorageService.getIdentifiers(params['id'])
    });
  }

  sort(property: string) {
    this.currentSort = this.currentSort === property ? '-' + property : property;
    this.identifiers.usedTransportEquipment = this.identifiers.usedTransportEquipment.sort(ArrayUtils.dynamicSort(this.currentSort));
  }

  sortCarriage(property: string) {
    this.currentSort = this.currentSort === property ? '-' + property : property;
    this.identifiers.mainCarriageTransportMovement = this.identifiers.mainCarriageTransportMovement.sort(ArrayUtils.dynamicSort(this.currentSort));
  }

  goToUil() {
    this.router.navigate(['/uil'],
      { queryParams: { id: this.identifiers.datasetId, gate: this.identifiers.gateId, platform: this.identifiers.platformId } }
    );
  }

  showCarried(i : number) {
    let elems = document.querySelectorAll("#carried-" + i);
    let index = 0, length = elems.length;
    for ( ; index < length; index++) {
      if(elems[index].classList.contains("hidden")) {
        elems[index].classList.remove('hidden')
      } else {
        elems[index].classList.add('hidden')
      }
    }
  }

  getTransportModeFromCode(index: any): string {
    return transportMode[index];
  }
}
