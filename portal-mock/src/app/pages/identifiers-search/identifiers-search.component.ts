import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet } from "@angular/router";
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from "@angular/forms";
import { DatePipe, KeyValuePipe, NgClass, NgIf, NgFor } from "@angular/common";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { IdentifiersService } from "../../core/services/identifiers.service";
import { SessionService } from "../../core/services/session.service";
import { CurrentSearch } from "../../core/models/current-search.model";
import { IconsModule } from "../../icons/icons.module";
import { ArrayUtils } from "../../core/utils/array-utils";
import { ToastrService } from "ngx-toastr";
import { IDropdownSettings, NgMultiSelectDropDownModule } from "ng-multiselect-dropdown";
import { IdentifiersResponse } from "../../core/models/identifiers-response.model";
import { LocalStorageService } from "../../core/services/local-storage.service";
import { IdentifiersSearch } from "../../core/models/identifiers-search.model";
import { Identifiers } from "../../core/models/identifiers.model";
import { Subscription, timer } from "rxjs";
import { NgbPopover } from "@ng-bootstrap/ng-bootstrap";
import { HighchartsChartModule } from 'highcharts-angular';
import * as Highcharts from 'highcharts/highmaps';
import europeMap from '../../../../files/files/custom_europa.json';
import { transportMode } from "../../core/models/transport-mode.model";
import { EnumSelectPipe } from "../../core/pipe/enum-select.pipe";

@Component({
  selector: 'app-identifiers-search',
  standalone: true,
  templateUrl: './identifiers-search.component.html',
  imports: [
    RouterOutlet, ReactiveFormsModule, NgFor, NgIf, NgClass,
    IconsModule, TranslateModule, DatePipe, NgMultiSelectDropDownModule, FormsModule, NgbPopover,
    HighchartsChartModule, KeyValuePipe, EnumSelectPipe
  ],
  styleUrl: './identifiers-search.component.css'
})
export class IdentifiersSearchComponent implements OnInit {

  protected readonly transportMode = transportMode;
  identifierTypes: Array<string> = ["means", "equipment", "carried"]

  countries =  ["AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT", "LV",
    "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "SY", "LI", "BO"]

  searchForm!: FormGroup;
  public formSubmitted = false;

  currentSearch: CurrentSearch = {} as CurrentSearch;
  public result!: IdentifiersResponse;
  public identifiers!: Identifiers[];
  currentSort!: string;

  highcharts: typeof Highcharts = Highcharts;

  chartOptions: Highcharts.Options = {
    chart: {
      map: europeMap
    },
    title: {
      text: "eFTI"
    }
  };

  updateFlag: boolean = false;

  constructor(private formBuilder: FormBuilder, public translate: TranslateService, private service: IdentifiersService,
              private sessionService: SessionService, private router: Router,
              private localStorageService: LocalStorageService, private toastr: ToastrService) {
    this.initForm();
  }
  selectedGateIndicators: string[] = [];
  selectedIdentifierType: string[] = [];
  dropdownSettings: IDropdownSettings = {};

  subscription !: Subscription;

  ngOnInit() {

    this.dropdownSettings = {
      singleSelection: false,
      idField: 'item_id',
      textField: 'item_text',
      selectAllText: 'Select All',
      unSelectAllText: 'UnSelect All',
      itemsShowLimit: 3,
      allowSearchFilter: false
    };

    this.subscription = timer(0, 2000).subscribe(() => {
      if(this.localStorageService.getAutoPolling() && this.currentSearch.status == 'PENDING') this.pollResult();
    });

    this.updateSeriesOption();
  }

  onItemSelectCountry(item: any) {
    this.selectedGateIndicators.push(item)
  }

  onItemSelectIdentifierType(item: any) {
    this.selectedIdentifierType.push(item)
  }

  onItemDeselectCountry(item: any) {
    this.selectedGateIndicators = this.selectedGateIndicators.filter(elem => elem !== item);
  }

  onItemDeselectIdentifierType(item: any) {
    this.selectedIdentifierType = this.selectedIdentifierType.filter(elem => elem !== item);
  }

  onSelectAllCountry() {
    this.selectedGateIndicators = this.countries;
  }

  onSelectAllIdentifierType() {
    this.selectedIdentifierType = this.identifierTypes;
  }

  onDeselectAllCountry() {
    this.selectedGateIndicators = [];
  }

  onDeselectAllIdentifierType() {
    this.selectedIdentifierType = [];
  }

  initForm() {
    this.searchForm = this.formBuilder.group({
      identifier: [ { value: null, disabled: false }, [Validators.required, Validators.pattern("[A-Za-z0-9]{0,17}$")] ],
      identifierType: [ { value: null, disabled: false } ],
      registrationCountryCode: [ { value: null, disabled: false } ],
      modeCode: [ { value: null, disabled: false } ],
      dangerousGoodsIndicator: [ { value: 'NA', disabled: false } , Validators.required ]
    });
  }

  hasFieldError(key: string): boolean {
    return this.formSubmitted && !this.searchForm.controls[key].valid;
  }

  getFieldError(field: string): string | null {
    if(this.searchForm.controls[field].hasError('required')) {
      return this.translate.instant('form.error.required')
    } else if (this.searchForm.controls[field].hasError('pattern')) {
      return this.translate.instant('form.error.pattern')
    }
    return null;
  }

  submit() {
    this.formSubmitted = true;
    if(!this.searchForm.valid) {
      return;
    }
    const searchData: IdentifiersSearch = {
      'modeCode': this.searchForm.controls['modeCode'].value,
      'identifier': this.searchForm.controls['identifier'].value,
      'identifierType': this.selectedIdentifierType.length > 0 ? this.selectedIdentifierType : [],
      'registrationCountryCode': this.searchForm.controls['registrationCountryCode'].value,
      'dangerousGoodsIndicator': this.getBooleanValue(this.searchForm.controls['dangerousGoodsIndicator'].value),
      'eftiGateIndicator': this.selectedGateIndicators.length > 0 ? this.selectedGateIndicators : [],
    };

    this.result = {} as IdentifiersResponse;
    this.currentSearch = {} as CurrentSearch;
    this.formSubmitted = false;

    this.service.postIdentifiersControl(searchData).subscribe({
      next: (response) => {
        this.currentSearch.requestId = response.requestId;
        this.currentSearch.status = response.status;
        this.toastr.success(this.translate.instant('identifiers-search.request-saved'));
      },
      error: () => {
        this.toastr.error(this.translate.instant('identifiers-search.error'));
      }
    })
  }

  reset() {
    this.searchForm.reset();
    this.result = {} as IdentifiersResponse;
    this.currentSearch = {} as CurrentSearch;
    this.formSubmitted = false;
  }

  pollResult(): void {
    this.service.getIdentifiersControl(this.currentSearch.requestId).subscribe({
      next: (result) => {
        this.manageResult(result);
        if(result.status != 'PENDING') {
          this.toastr.success(this.translate.instant('identifiers-search.request-updated'));
        }
        this.updateSeriesOption();
      },
      error: () => {
        this.toastr.error(this.translate.instant('identifiers-search.error'));
      }
    });
  }

  manageResult(result: IdentifiersResponse) {
    this.result = result;
    this.identifiers = [];
    result.identifiers.forEach(i => {
      this.identifiers = [...this.identifiers, ...i.consignments];
    })
    this.currentSearch.status = result.status;
  }
  displayIdentifiers(identifiers: Identifiers): void {
    this.localStorageService.addIdentifiers(identifiers);
    const url = this.router.serializeUrl(
      this.router.createUrlTree(['/identifiers-display/' + identifiers.datasetId])
    );
    window.open(url, '_blank');
  }

  private getBooleanValue(value: string) : boolean | null {
    if(value === 'YES') return true;
    if(value === 'NO') return false;
    return null;
  }

  sort(property: string) {
    this.currentSort = this.currentSort === property ? '-' + property : property;
    this.result.identifiers = this.result.identifiers.sort(ArrayUtils.dynamicSort(this.currentSort));
  }

  getClassFromStatus(status: string) {
    if(status == 'COMPLETE') {
      return "complete";
    } else if (status == 'ERROR') {
      return "error";
    } else if (status == 'TIMEOUT') {
      return "timeout";
    }
    return "pending";
  }

  private updateSeriesOption() {

    let notCalled: [string, number][] = [];
    let inProgress: [string, number][] = [];
    let success: [string, number][] = [];
    let error: [string, number][] = [];
    let timeout: [string, number][] = [];

    this.countries.forEach(country => {

      if(!this.result) {
        notCalled.push([country.toLowerCase(), 0]);
      } else {
        let foundRaw = this.result.identifiers.filter(idt => idt.gateIndicator == country)[0];
        if(!foundRaw) {
          notCalled.push([country.toLowerCase(), 0]);
        } else {
          switch(foundRaw.status) {
            case 'PENDING':
              inProgress.push([country.toLowerCase(), 0]);
              break;
            case 'COMPLETE':
              success.push([country.toLowerCase(), this.result?.identifiers.filter(id => id.gateIndicator === country).length]);
              break;
            case 'TIMEOUT':
              timeout.push([country.toLowerCase(), 0]);
              break;
            case 'ERROR':
              error.push([country.toLowerCase(), 0]);
              break;
          }
        }
      }
    })

    this.chartOptions.series = [
      {
        type: 'map',
        name: 'Not called',
        allAreas: false,
        data: notCalled,
        color: 'grey'
      },
      {
        type: 'map',
        name: 'In Progress',
        allAreas: false,
        data: inProgress,
        color: '#003088'
      },
      {
        type: 'map',
        name: 'Success',
        allAreas: false,
        data: success,
        color: 'green'
      },
      {
        type: 'map',
        name: "Error",
        allAreas: false,
        data: error,
        color: 'red'
      },
      {
        type: 'map',
        name: 'Timeout',
        allAreas: false,
        data: timeout,
        color: '#ff9900'
      },
    ]
    this.updateFlag = true;
  }

}
