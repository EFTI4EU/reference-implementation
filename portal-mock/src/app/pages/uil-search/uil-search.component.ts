import {Component, inject, OnInit, TemplateRef} from '@angular/core';
import {ActivatedRoute, RouterOutlet} from "@angular/router";
import {DatePipe, NgClass, NgForOf, NgIf} from "@angular/common";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import {IconsModule} from "../../icons/icons.module";
import {UilSearchModel} from "../../core/models/uil-search.model";
import {UilService} from "../../core/services/uil.service";
import {UilResponse} from "../../core/models/uil-response.model";
import {ToastrService} from "ngx-toastr";
import {DomSanitizer} from "@angular/platform-browser";
import {PlatformModel} from "../../core/models/platform.model";
import {NgSelectModule} from "@ng-select/ng-select";
import {UilResult} from "./uil-result-model";
import {NgbModal, NgbPopoverModule} from '@ng-bootstrap/ng-bootstrap';
import {NoteService} from "../../core/services/note.service";
import {RequestIdModel} from "../../core/models/RequestId.model";
import {Subscription, timer} from "rxjs";
import {LocalStorageService} from "../../core/services/local-storage.service";
import FileSaver from 'file-saver';

@Component({
  selector: 'app-uil-search',
  standalone: true,
  templateUrl: './uil-search.component.html',
  imports: [
    RouterOutlet, IconsModule, DatePipe, NgForOf, NgIf, ReactiveFormsModule,
    TranslateModule, NgClass, FormsModule, NgSelectModule, NgbPopoverModule
  ],
  styleUrl: './uil-search.component.css'
})
export class UilSearchComponent implements OnInit {

  private modalService = inject(NgbModal);
  note: string = '';

  gates: Array<string> = ["france", "borduria", "syldavia"];

  platforms: Array<PlatformModel> = [
    { "id": "ttf", "label": "ttf - FR"},
    { "id": "acme", "label": "acme - BO"},
    { "id": "massivedynamic", "label": "massivedynamic - SY"}];

  searchForm!: FormGroup;
  public formSubmitted = false;

  public result: UilResult[] = [];

  subscription !: Subscription;

  constructor(private formBuilder: FormBuilder, public translate: TranslateService, private service: UilService, private toastr: ToastrService,
              private sanitizer: DomSanitizer, private route: ActivatedRoute, private noteService: NoteService,
              private localStorageService: LocalStorageService) {
    const id: string = this.route.snapshot.queryParamMap.get('id')!;
    const gate: string = this.route.snapshot.queryParamMap.get('gate')!;
    const platform: string = this.route.snapshot.queryParamMap.get('platform')!;
    this.initForm(id, gate, platform);

  }

  ngOnInit() {
    this.subscription = timer(0, 2000).subscribe(() => {
      if(this.localStorageService.getAutoPolling()) this.autoPoll();
    });
  }

  initForm(id: string , gate: string, platform: string) {
    this.searchForm = this.formBuilder.group({
      id: [ { value: id, disabled: false }, [Validators.required, Validators.pattern("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")] ],
      gate: [ { value: gate, disabled: false } ],
      platform: [ { value: platform, disabled: false } ]
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

  reset() {
    this.searchForm.reset();
    this.formSubmitted = false;
  }

  submit() {
    this.formSubmitted = true;
    if (!this.searchForm.valid) {
      return;
    }

    const searchData: UilSearchModel = {
      'datasetId': this.searchForm.controls['id'].value,
      'platformId': this.searchForm.controls['platform'].value,
      'gateId': this.searchForm.controls['gate'].value
    };

    this.formSubmitted = false;

    this.service.postUilControl(searchData).subscribe({
      next: (response) => {
        this.addNewEntry(response, searchData);
        this.toastr.success(this.translate.instant('identifiers-search.request-saved'));
      },
      error: () => {
        this.toastr.error(this.translate.instant('identifiers-search.error'));
      }
    })
  }

  autoPoll() {
    this.result.forEach((result) => {
      if(result.status == 'PENDING') {
        this.pollResult(result.requestId);
      }
    })
  }

  pollResult(requestId: string): void {
    this.service.getUilControl(requestId).subscribe({
      next: (result) => {
        this.updateEntry(result);
        if(result.status != 'PENDING') {
          this.toastr.success(this.translate.instant('identifiers-search.request-updated'));
        }
      },
      error: () => {
        this.toastr.error(this.translate.instant('identifiers-search.error'));
      }
    });
  }

  addNewEntry(entry: RequestIdModel, search: UilSearchModel) {
    this.result.push({
      'requestId': entry.requestId,
      'status': entry.status,
      'datasetId': search.datasetId,
      'gateId': search.gateId,
      'platformId': search.platformId,
      'errorCode': entry.errorCode,
      'errorDescription': entry.errorDescription
      } as UilResult);
  }

  updateEntry(response: UilResponse) {
    let indexToUpdate = this.result.findIndex(entry => entry.requestId === response.requestId);
    this.result[indexToUpdate].data = response.data;
    this.result[indexToUpdate].status = response.status;
    this.result[indexToUpdate].errorCode = response.errorCode;
    this.result[indexToUpdate].errorDescription = response.errorDescription;
  }

  async open(requestId: string) {
    let index = this.result.findIndex(entry => entry.requestId === requestId);
    let content = atob(this.result[index].data);
    const style = await this.getAsset('/assets/xslt/eCMR.xslt');
    const styleEl = this.parse(style);
    console.log(styleEl);
    const contentEl = this.parse(content);
    console.log(contentEl);
    const xsltProcessor = new XSLTProcessor();
    xsltProcessor.importStylesheet(styleEl);
    const resultDocument = xsltProcessor.transformToDocument(contentEl);

    if(resultDocument.firstElementChild != null) {
      let winUrl = URL.createObjectURL(new Blob([resultDocument.firstElementChild.innerHTML], {type: 'text/html'}));
      window.open(winUrl);
    }
  }

  download(requestId: string) {
    let index = this.result.findIndex(entry => entry.requestId === requestId);
    let content = atob(this.result[index].data);
    const file = new Blob([content], {type: 'text/text'});
    const filename = this.result[index].datasetId + ".xml"
    FileSaver.saveAs(file, filename);
  }

  parse(data: string): HTMLElement {
    const parser = new DOMParser();
    const xmlNode = parser.parseFromString(data, 'application/xml');
    return xmlNode.documentElement;
  }

  async getAsset(file: string): Promise<string> {
    const res = await fetch(file);
    return await res.text();
  }

  clear() {
    this.result = [];
  }

  openModal(content: TemplateRef<any>, request: UilResult) {
    this.modalService.open(content, { ariaLabelledBy: 'modal-basic-title' }).result.then(
      () => {
        let noteRequest = {
          'requestId': request.requestId,
          'message': this.note
        };
        this.noteService.postNote(noteRequest).subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('identifiers-search.note-saved'));
            this.note = '';
          },
          error: () => {
            this.toastr.error(this.translate.instant('identifiers-search.error'));
          }
        })
      },
      () => {
        this.note = '';
      },
    );
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
}
