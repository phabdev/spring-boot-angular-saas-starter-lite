import { randomBytes } from 'node:crypto';
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';

const target = resolve(process.cwd(), '.env');
if (existsSync(target)) {
  console.error('.env already exists; preserving it. Edit the existing file deliberately.');
  process.exit(1);
}
const example = readFileSync(resolve(process.cwd(), '.env.example'), 'utf8');
const configured = example
  .replace(/^DB_PASSWORD=.*$/m, `DB_PASSWORD=${randomBytes(24).toString('hex')}`)
  .replace(/^JWT_SECRET=.*$/m, `JWT_SECRET=${randomBytes(48).toString('base64url')}`);
if (!/^DB_PASSWORD=.{32,}$/m.test(configured) || !/^JWT_SECRET=.{32,}$/m.test(configured)) {
  throw new Error('Run this initializer from a Lite or Pro edition directory.');
}
writeFileSync(target, configured, { flag: 'wx', mode: 0o600 });
console.log('Created .env with random local secrets. Keep this file private.');
