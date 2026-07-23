import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

test('contains the intentional image fixture', () => {
  const source = fs.readFileSync(new URL('../src/App.jsx', import.meta.url), 'utf-8');
  assert.match(source, /<img src="sample-product\.png" \/>/);
});
