// Fresh isolated backup/restore only. Never drops or migrates the source database.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {execFileSync} = require('node:child_process');
const root = path.resolve(__dirname, '../..');
const args = process.argv.slice(2);
if (args.includes('--help')) {
  console.log('node scripts/acceptance/prepare.cjs --documents <physical document directory> [--documents <another directory>]');
  process.exit(0);
}
const documents = [];
for (let i = 0; i < args.length; i++) {
  if (args[i] !== '--documents' || !args[i + 1] || args[i + 1].startsWith('--')) throw Error('Only --documents <directory> is accepted; use --help');
  const folder = path.resolve(root, args[++i]);
  if (!fs.statSync(folder).isDirectory()) throw Error('Document directory missing');
  documents.push(folder);
}
if (!documents.length) throw Error('Provide all physical document directories; use --help');
const runId = new Date().toISOString().slice(0, 10).replaceAll('-', '') + '-' + crypto.randomBytes(4).toString('hex');
const dir = path.join(root, 'storage/acceptance', runId);
fs.mkdirSync(dir, {recursive: true});
const runtime = {runId, dir, prefix: 'ACC' + crypto.randomBytes(5).toString('hex'), basePort: 28080, frontendPort: 25173, browserPort: 29388, createdAt: new Date().toISOString(), backup: path.join(dir, 'backup')};
const fd = fs.openSync(path.join(dir, 'prepare.log'), 'a');
function command(args) {
  execFileSync(process.execPath, [path.join(root, 'scripts/operations.cjs'), ...args], {cwd: root, windowsHide: true, stdio: ['ignore', fd, fd]});
}
try {
  command(['backup', '--online-rehearsal', '--output', runtime.backup, ...documents.flatMap(d => ['--documents', d])]);
  command(['rehearse', '--backup', runtime.backup]);
  const rehearsal = JSON.parse(fs.readFileSync(path.join(runtime.backup, 'rehearsal.json'), 'utf8'));
  if (!/^lc_rehearsal_[a-zA-Z0-9_]+$/.test(rehearsal.database)) throw Error('Refuse non-rehearsal database');
  runtime.database = rehearsal.database;
  fs.copyFileSync(path.join(__dirname, 'expected.json'), path.join(dir, 'expected.json'));
  fs.writeFileSync(path.join(dir, 'runtime.json'), JSON.stringify(runtime, null, 2));
  // Publish latest only after backup, restore and migrations have all succeeded.
  fs.writeFileSync(path.join(root, 'storage/acceptance/latest.txt'), dir);
  console.log('Prepared ' + dir + '\nDatabase: ' + runtime.database + '\nSource unchanged; no automatic DROP. See prepare.log.');
} finally {
  fs.closeSync(fd);
}
