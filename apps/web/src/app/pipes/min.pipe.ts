import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
  name: "min"
})
export class MinPipe implements PipeTransform {

  transform(value: number, min: number): unknown {
    return Math.max(value, min);
  }

}
