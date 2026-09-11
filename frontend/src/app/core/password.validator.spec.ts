import { FormControl } from '@angular/forms';
import { passwordByteLimit } from './password.validator';

describe('BCrypt browser password limit', () => {
  it('allows 72 ASCII bytes and rejects the next byte', () => {
    expect(passwordByteLimit(new FormControl('a'.repeat(72)))).toBeNull();
    expect(passwordByteLimit(new FormControl('a'.repeat(73)))).toEqual({ passwordBytes: true });
  });
  it('counts multibyte characters by UTF-8 bytes', () => {
    expect(passwordByteLimit(new FormControl('é'.repeat(36)))).toBeNull();
    expect(passwordByteLimit(new FormControl('é'.repeat(37)))).toEqual({ passwordBytes: true });
  });
});
