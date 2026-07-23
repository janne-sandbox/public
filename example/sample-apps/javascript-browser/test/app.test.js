import assert from 'node:assert/strict';
import test from 'node:test';

import { greeting } from '../src/app.js';

test('creates a greeting', () => {
  assert.equal(greeting('review sample'), 'Hello, review sample');
});
