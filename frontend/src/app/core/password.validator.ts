import { AbstractControl, ValidationErrors } from '@angular/forms';

/** BCrypt rejects inputs longer than 72 UTF-8 bytes, including multibyte text. */
export function passwordByteLimit(control: AbstractControl): ValidationErrors | null {
  return new TextEncoder().encode(control.value ?? '').length > 72 ? { passwordBytes: true } : null;
}
