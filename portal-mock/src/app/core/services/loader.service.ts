import {Injectable} from '@angular/core';
import {Subject} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class LoaderService {

  private loaderEl!: HTMLDialogElement;
  private loadingCnt = 0;
  public isLoading = new Subject<boolean>();

  public show(): void {
    this.loadingCnt++;
    const loaderEl: HTMLDialogElement = this.getLoaderElement();
    if (!loaderEl?.open && this.loadingCnt > 0) {
      loaderEl?.showModal();
    }
    this.isLoading.next(this.loadingCnt>0);
  }

  public hide(): void {
    this.loadingCnt--;
    if (this.loadingCnt <= 0) {
      this.getLoaderElement()?.close();
    }
    this.isLoading.next(this.loadingCnt>0);
  }

  private getLoaderElement(): HTMLDialogElement {
    if (!this.loaderEl) {
      this.loaderEl = document.querySelector('.loading-container') as HTMLDialogElement;
    }

    return this.loaderEl;
  }
}
