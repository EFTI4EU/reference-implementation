import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  standalone: true,
  name: 'enum'
})
export class EnumSelectPipe implements PipeTransform {
  transform(value: any): [number, string][] {
    return Object.keys(value).filter(t => isNaN(+t)).map(t => [value[t], t]);
  }
}
