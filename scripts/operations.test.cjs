const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');
const {identifier, options, verify, safeFile, main} = require('./operations.cjs');
function fixture() {
  const folder = fs.mkdtempSync(path.join(os.tmpdir(), 'lc-backup-test-'));
  const data = Buffer.from('-- example SQL\n');
  fs.writeFileSync(path.join(folder, 'database.sql'), data);
  const manifest = {format: 1, complete: true, files: [{path: 'database.sql', bytes: data.length, sha256: crypto.createHash('sha256').update(data).digest('hex')}]};
  const save = () => fs.writeFileSync(path.join(folder, 'manifest.json'), JSON.stringify(manifest));
  save();
  return {folder, manifest, save};
}
test('identifiers reject SQL and qualified schema names', () => {
  assert.equal(identifier('lc_rehearsal_123'), '`lc_rehearsal_123`');
  for (const value of ['a;DROP DATABASE x', 'a.b', '`x`', '']) assert.throws(() => identifier(value));
});
test('repeated document roots remain distinct arguments', () => {
  assert.deepEqual(options(['--documents', 'first root', '--documents', 'second root', '--online-rehearsal']), {documents: ['first root', 'second root'], 'online-rehearsal': [true]});
});
test('valid backup verifies actual file bytes', async () => { const f = fixture(); assert.equal((await verify(f.folder)).complete, true); });
test('changed SQL is rejected before restore', async () => { const f = fixture(); fs.appendFileSync(path.join(f.folder, 'database.sql'), 'x'); await assert.rejects(verify(f.folder), /checksum/); });
test('incomplete backup is rejected', async () => { const f = fixture(); f.manifest.complete = false; f.save(); await assert.rejects(verify(f.folder), /Incomplete/); });
test('manifest traversal and duplicate entries are rejected', async () => {
  const f = fixture(); f.manifest.files.push({...f.manifest.files[0], path: '../outside.sql'}); f.save(); await assert.rejects(verify(f.folder), /Invalid/);
  f.manifest.files.pop(); f.manifest.files.push({...f.manifest.files[0]}); f.save(); await assert.rejects(verify(f.folder), /Exactly one/);
});
test('missing backed-up file is rejected', async () => {
  const f = fixture(); f.manifest.files.push({path: 'documents/' + 'a'.repeat(36), bytes: 1, sha256: '0'.repeat(64)}); f.save(); await assert.rejects(verify(f.folder));
});
test('absolute and parent paths never resolve as backup entries', () => {
  const f = fixture(); assert.throws(() => safeFile(f.folder, '../manifest.json')); assert.throws(() => safeFile(f.folder, path.resolve(f.folder, 'database.sql')));
});
test('backup requires a declared consistency mode before connecting', async () => { await assert.rejects(main(['backup', '--env', 'nonexistent']), /Pause all writers/); });
