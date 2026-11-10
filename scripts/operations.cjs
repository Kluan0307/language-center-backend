// Node 22+, no npm dependencies. Never prints credentials or restores over a live schema.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {spawn} = require('node:child_process');
const {pipeline} = require('node:stream/promises');
const root = path.resolve(__dirname, '..');
const sha = value => crypto.createHash('sha256').update(value).digest('hex');
const identifier = value => {
  if (!/^[A-Za-z0-9_]+$/.test(value || '')) throw Error('Invalid SQL identifier');
  return '`' + value + '`';
};
function options(args) {
  const result = {};
  for (let i = 0; i < args.length; i++) {
    const key = args[i];
    if (!key.startsWith('--')) throw Error('Expected --option');
    const value = args[i + 1] && !args[i + 1].startsWith('--') ? args[++i] : true;
    (result[key.slice(2)] ??= []).push(value);
  }
  return result;
}
function setting(o, key, fallback) { return o[key]?.at(-1) ?? fallback; }
function loadEnvironment(file) {
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '').split(/\r?\n/)) {
    const m = line.match(/^([A-Z][A-Z0-9_]*)=(.*)$/);
    if (m && process.env[m[1]] === undefined) process.env[m[1]] = m[2];
  }
}
function dbConfig() {
  if (!process.env.DB_URL?.startsWith('jdbc:mysql://')) throw Error('DB_URL must be a MySQL JDBC URL');
  const url = new URL(process.env.DB_URL.slice(5));
  const database = decodeURIComponent(url.pathname.slice(1));
  identifier(database);
  if (!process.env.DB_USERNAME || !process.env.DB_PASSWORD) throw Error('DB_USERNAME and DB_PASSWORD required');
  const binaryDir = process.env.MYSQL_BIN || (process.platform === 'win32' ? 'C:/Program Files/MySQL/MySQL Server 8.0/bin' : '');
  const binary = name => binaryDir ? path.join(binaryDir, name + (process.platform === 'win32' ? '.exe' : '')) : name;
  return {database, binary, args: ['--host=' + url.hostname, '--port=' + (url.port || '3306'), '--user=' + process.env.DB_USERNAME, '--default-character-set=utf8mb4']};
}
async function run(binary, args, inputFile) {
  const child = spawn(binary, args, {shell: false, windowsHide: true, env: {...process.env, MYSQL_PWD: process.env.DB_PASSWORD}, stdio: ['pipe', 'pipe', 'pipe']});
  let output = '', error = '';
  child.stdout.on('data', c => { output += c; });
  child.stderr.on('data', c => { error += c; });
  const exited = new Promise((resolve, reject) => {
    child.on('error', reject);
    child.on('close', code => code === 0 ? resolve(output.trim()) : reject(Error('MySQL operation failed; check connectivity, grants and schema (exit ' + code + ')')));
  });
  // Handle child failure while piping a large dump, without an unhandled rejection.
  const writing = inputFile ? pipeline(fs.createReadStream(inputFile), child.stdin) : (child.stdin.end(), Promise.resolve());
  const results = await Promise.allSettled([exited, writing]);
  for (const result of results) if (result.status === 'rejected') throw result.reason;
  return results[0].value;
}
async function sql(db, statement, database = db.database) {
  return run(db.binary('mysql'), [...db.args, '--batch', '--raw', '--skip-column-names', '--database=' + database, '--execute=' + statement]);
}
async function hashFile(file) {
  const hash = crypto.createHash('sha256');
  for await (const chunk of fs.createReadStream(file)) hash.update(chunk);
  return hash.digest('hex');
}
function safeFile(folder, relative) {
  if (typeof relative !== 'string' || relative.includes('\\') || path.isAbsolute(relative)) throw Error('Unsafe manifest path');
  const target = path.resolve(folder, relative);
  if (!target.startsWith(path.resolve(folder) + path.sep)) throw Error('Manifest path escapes backup');
  const stat = fs.lstatSync(target);
  if (!stat.isFile() || stat.isSymbolicLink()) throw Error('Backup entry must be a regular file');
  if (!fs.realpathSync(target).startsWith(fs.realpathSync(folder) + path.sep)) throw Error('Backup entry resolves outside backup');
  return target;
}
async function tableCounts(db, database = db.database) {
  const tables = (await sql(db, 'SHOW FULL TABLES WHERE Table_type = \'BASE TABLE\'', database)).split(/\r?\n/).filter(Boolean).map(line => line.split('\t')[0]);
  const result = {};
  for (const table of tables.sort()) result[table] = Number(await sql(db, 'SELECT COUNT(*) FROM ' + identifier(table), database));
  return result;
}
async function dataFingerprint(db, database = db.database) {
  const data = await run(db.binary('mysqldump'), [...db.args, '--single-transaction', '--quick', '--no-create-info', '--skip-comments', '--skip-add-locks', '--skip-disable-keys', '--skip-extended-insert', '--compact', '--order-by-primary', '--no-tablespaces', '--set-gtid-purged=OFF', '--hex-blob', database]);
  return sha(data.replaceAll('\r\n', '\n'));
}
async function backup(o) {
  if (!o['writes-paused'] && !o['online-rehearsal']) throw Error('Pause all writers and pass --writes-paused; --online-rehearsal creates an unqualified rehearsal snapshot only');
  const roots = (o.documents || []).map(p => path.resolve(String(p)));
  if (!roots.length) throw Error('Provide --documents for each physical document directory');
  for (const directory of roots) if (!fs.statSync(directory).isDirectory()) throw Error('Document directory missing');
  const db = dbConfig();
  const folder = path.resolve(String(setting(o, 'output', path.join(root, 'storage/backups', 'backup-' + Date.now() + '-' + crypto.randomBytes(3).toString('hex')))));
  // Refuse overwriting even an empty existing backup directory.
  fs.mkdirSync(path.dirname(folder), {recursive: true});
  fs.mkdirSync(folder, {recursive: false});
  fs.mkdirSync(path.join(folder, 'documents'));
  const manifest = {format: 1, complete: false, database: db.database, createdAt: new Date().toISOString(), consistency: o['writes-paused'] ? 'operator-confirmed-writes-paused' : 'online-rehearsal-unqualified', files: [], rowCounts: await tableCounts(db), dataSha256: await dataFingerprint(db)};
  fs.writeFileSync(path.join(folder, 'manifest.json'), JSON.stringify(manifest, null, 2));
  const dump = path.join(folder, 'database.sql');
  await run(db.binary('mysqldump'), [...db.args, '--single-transaction', '--quick', '--skip-lock-tables', '--no-tablespaces', '--set-gtid-purged=OFF', '--hex-blob', '--result-file=' + dump, db.database]);
  manifest.files.push({path: 'database.sql', bytes: fs.statSync(dump).size, sha256: await hashFile(dump)});
  const copied = new Map();
  for (const directory of roots) {
    for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
      if (!entry.isFile() || !/^[0-9a-f-]{36}$/.test(entry.name)) throw Error('Unexpected document entry; backup requires UUID files and no links/subdirectories');
      const source = path.join(directory, entry.name), hash = await hashFile(source);
      if (copied.has(entry.name)) {
        if (copied.get(entry.name) !== hash) throw Error('Conflicting document key across roots');
        continue;
      }
      const relative = 'documents/' + entry.name, target = path.join(folder, relative);
      fs.copyFileSync(source, target, fs.constants.COPYFILE_EXCL);
      if (await hashFile(target) !== hash || await hashFile(source) !== hash) throw Error('Document changed while copying');
      copied.set(entry.name, hash);
      manifest.files.push({path: relative, bytes: fs.statSync(target).size, sha256: hash});
    }
  }
  if (JSON.stringify(await tableCounts(db)) !== JSON.stringify(manifest.rowCounts)) throw Error('Database row counts changed during backup; pause writers and retry');
  if (manifest.dataSha256 !== await dataFingerprint(db)) throw Error('Database records changed during backup; pause writers and retry');
  manifest.complete = true;
  fs.writeFileSync(path.join(folder, 'manifest.json'), JSON.stringify(manifest, null, 2));
  console.log(JSON.stringify({backup: folder, consistency: manifest.consistency, files: manifest.files.length}));
  return folder;
}
async function verify(folder) {
  folder = path.resolve(folder);
  const manifest = JSON.parse(fs.readFileSync(path.join(folder, 'manifest.json'), 'utf8'));
  if (manifest.format !== 1 || manifest.complete !== true || !Array.isArray(manifest.files) || !manifest.files.length) throw Error('Incomplete or unsupported backup');
  if (manifest.files.filter(f => f.path === 'database.sql').length !== 1) throw Error('Exactly one SQL dump required');
  const seen = new Set();
  for (const file of manifest.files) {
    if (seen.has(file.path) || !/^(database\.sql|documents\/[0-9a-f-]{36})$/.test(file.path)) throw Error('Invalid/duplicate manifest entry');
    seen.add(file.path);
    const target = safeFile(folder, file.path);
    if (fs.statSync(target).size !== file.bytes || await hashFile(target) !== file.sha256) throw Error('Backup checksum mismatch: ' + file.path);
  }
  return manifest;
}
async function fileReferences(db, database, folder, manifest) {
  const keys = (await sql(db, 'SELECT storage_key FROM documents UNION SELECT storage_key FROM document_events', database)).split(/\r?\n/).filter(Boolean);
  const included = new Set(manifest.files.map(f => f.path));
  for (const key of keys) {
    if (!/^[0-9a-f-]{36}$/.test(key) || !included.has('documents/' + key)) throw Error('Restored document revision has no backed-up file');
    safeFile(folder, 'documents/' + key);
  }
  return keys.length;
}
async function rehearse(o) {
  const folder = path.resolve(String(setting(o, 'backup', ''))), manifest = await verify(folder), db = dbConfig();
  // Target is always a generated new schema, never the configured source schema.
  const database = 'lc_rehearsal_' + Date.now() + '_' + crypto.randomBytes(3).toString('hex');
  const dump = fs.readFileSync(path.join(folder, 'database.sql'), 'utf8');
  if (/^\s*(?:\/\*!\d+\s+)?(?:USE\s|(?:CREATE|DROP|ALTER)\s+DATABASE\b)/im.test(dump)) throw Error('Dump must not switch/create/drop databases');
  const original = await sql(db, 'SELECT id,password,session_version,must_change_password FROM accounts ORDER BY id');
  await sql(db, 'CREATE DATABASE ' + identifier(database) + ' CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');
  await run(db.binary('mysql'), [...db.args, '--database=' + database], path.join(folder, 'database.sql'));
  const restoredCounts = await tableCounts(db, database);
  if (JSON.stringify(restoredCounts) !== JSON.stringify(manifest.rowCounts)) throw Error('Restore row counts differ from backup');
  if (!manifest.dataSha256 || manifest.dataSha256 !== await dataFingerprint(db, database)) throw Error('Restored data fingerprint differs from backup');
  const referencedFiles = await fileReferences(db, database, folder, manifest);
  const restoredDocuments = path.join(folder, database + '-documents');
  fs.mkdirSync(restoredDocuments);
  for (const file of manifest.files.filter(f => f.path.startsWith('documents/'))) {
    const target = path.join(restoredDocuments, path.basename(file.path));
    fs.copyFileSync(safeFile(folder, file.path), target, fs.constants.COPYFILE_EXCL);
    if (await hashFile(target) !== file.sha256) throw Error('Restored document checksum differs');
  }
  const migrationDir = path.join(root, 'docs/migrations');
  const migrations = fs.readdirSync(migrationDir).filter(f => /^V\d{3}__.*\.sql$/.test(f)).sort();
  const beforeReplay = await sql(db, 'SELECT id,version,status,assigned_to,next_contact_at FROM leads ORDER BY id; SELECT id,version,revision,status,storage_key FROM documents ORDER BY id; SELECT id,password,session_version,must_change_password FROM accounts ORDER BY id', database);
  for (const file of migrations) await run(db.binary('mysql'), [...db.args, '--database=' + database], path.join(migrationDir, file));
  const afterReplay = await sql(db, 'SELECT id,version,status,assigned_to,next_contact_at FROM leads ORDER BY id; SELECT id,version,revision,status,storage_key FROM documents ORDER BY id; SELECT id,password,session_version,must_change_password FROM accounts ORDER BY id', database);
  if (beforeReplay !== afterReplay) throw Error('Migration replay changed protected restored records');
  if (original !== await sql(db, 'SELECT id,password,session_version,must_change_password FROM accounts ORDER BY id')) throw Error('Source credentials changed during rehearsal');
  await fileReferences(db, database, folder, manifest);
  const report = {database, sourceDatabaseUntouched: true, restoredDocuments, restoredAt: new Date().toISOString(), tables: Object.keys(restoredCounts).length, referencedFiles, migrations, consistency: manifest.consistency, migrationSha256: Object.fromEntries(migrations.map(f => [f, sha(fs.readFileSync(path.join(migrationDir, f)))])), note: 'Rehearsal database retained; no automatic DROP. Online rehearsal is not a qualified production recovery point.'};
  fs.writeFileSync(path.join(folder, 'rehearsal.json'), JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report));
}
async function smoke(o) {
  const base = Number(setting(o, 'base-port', 8080));
  if (!Number.isInteger(base) || base < 1 || base > 65531) throw Error('Invalid port');
  let checks = 0;
  for (let offset = 0; offset < 5; offset++) {
    for (const probe of ['liveness', 'readiness']) {
      const response = await fetch(`http://127.0.0.1:${base + offset}/actuator/health/${probe}`, {signal: AbortSignal.timeout(10000)});
      const body = await response.json();
      if (!response.ok || body.status !== 'UP' || body.components || body.details) throw Error('Health probe failed at offset ' + offset);
      checks++;
    }
    const response = await fetch(`http://127.0.0.1:${base + offset}/actuator/env`, {signal: AbortSignal.timeout(10000)});
    if (![401, 403, 404].includes(response.status)) throw Error('Sensitive management endpoint exposed');
    checks++;
  }
  for (const service of ['auth/teachers', 'academic/leads', 'teaching/documents', 'finance/tuition']) {
    const response = await fetch(`http://127.0.0.1:${base}/api/v1/${service}`, {signal: AbortSignal.timeout(10000)});
    if (response.status !== 401) throw Error('Unauthenticated business route not protected: ' + service);
    if (!/^[A-Za-z0-9-]{1,64}$/.test(response.headers.get('x-request-id') || '')) throw Error('Missing request correlation');
    checks++;
  }
  console.log(JSON.stringify({checks, basePort: base, passed: true}));
}
async function main(args = process.argv.slice(2)) {
  const [command, ...rest] = args, o = options(rest);
  loadEnvironment(path.resolve(String(setting(o, 'env', path.join(root, '.env.local')))));
  if (command === 'backup') return backup(o);
  if (command === 'verify') { const m = await verify(String(setting(o, 'backup', ''))); console.log(JSON.stringify({verified: true, files: m.files.length, consistency: m.consistency})); return; }
  if (command === 'rehearse') return rehearse(o);
  if (command === 'smoke') return smoke(o);
  throw Error('Usage: node scripts/operations.cjs backup|verify|rehearse|smoke --option value (see deployment runbook)');
}
module.exports = {identifier, options, safeFile, verify, main};
if (require.main === module) main().catch(error => { console.error(error.message); process.exitCode = 1; });
