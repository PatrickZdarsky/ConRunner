import {AbstractControl, ValidationErrors, ValidatorFn} from "@angular/forms";

export function descriptionLengthValidator(): ValidatorFn {
  return (control: AbstractControl<string | null>): ValidationErrors | null => {
    const length = control.value?.length ?? 0;
    return length > 1024 ? {tooLong: {value: control.value}} : null;
  };
}

export function captionLengthValidator(): ValidatorFn {
  return (control: AbstractControl<string | null>): ValidationErrors | null => {
    const length = control.value?.length ?? 0;
    return length > 255 ? {tooLong: {value: control.value}} : null;
  };
}

export function nameLengthValidator(): ValidatorFn {
  return (control: AbstractControl<string | null>): ValidationErrors | null => {
    const length = control.value?.length ?? 0;
    return length > 255 ? {tooLong: {value: control.value}} : null;
  };
}
