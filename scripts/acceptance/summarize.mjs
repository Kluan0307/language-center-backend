import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {dir, root, runtime, expected, jsonRead} from './client.mjs';
const sha = b => crypto.createHash('sha256').update(b).digest('hex');
const phases = [];
for (const [phase, count] of Object.entries(expected.requiredChecks)) {
  const report = jsonRead(path.join(dir, phase + '.json'));
  assert.equal(report.status, 'PASSED', phase);
  assert.equal(report.runId, runtime.runId, phase);
  assert.equal(report.database, runtime.database, phase);
  assert.equal(report.checks.length, count, phase);
  assert.ok(report.checks.every(c => c.passed && JSON.stringify(c.expected) === JSON.stringify(c.actual)), phase);
  phases.push({phase, checks: count, status: report.status, finishedAt: report.finishedAt});
}
// Credentials and the source database dump are intentionally excluded from the public inventory.
const files = fs.readdirSync(dir).filter(f => /^(g17|g18|g17-crud|browser|database)\.json$|^expected\.json$|^fixture\.json$|^http\.jsonl$|\.png$|^lifecycle\.log$/.test(f));
const inventory = files.map(file => ({file, bytes: fs.statSync(path.join(dir, file)).size, sha256: sha(fs.readFileSync(path.join(dir, file)))}));
const harness = fs.readdirSync(path.join(root, 'scripts/acceptance')).filter(f => /\.(mjs|cjs|ps1|json)$/.test(f)).map(file => ({file, sha256: sha(fs.readFileSync(path.join(root, 'scripts/acceptance', file)))}));
const jarDir=runtime.releaseRoot?path.join(runtime.releaseRoot,'backend'):path.join(dir,'jars');
const jars = fs.readdirSync(jarDir).filter(f => f.endsWith('.jar')).map(file => ({file, sha256: sha(fs.readFileSync(path.join(jarDir, file)))}));
assert.equal(jars.length, 5);
const result = {runId: runtime.runId, database: runtime.database, status: 'PASSED', checks: phases.reduce((n,p) => n + p.checks, 0), phases, generatedAt: new Date().toISOString(), inventory, harnessAtHandoff: harness, runtimeJars: jars, limits: ['Local gateway/MySQL/Edge acceptance, not Internet/TLS/load qualification', 'Failed attempts retained separately; final reports contain successful attempts', 'Source dump and private-session.json must stay local; do not publish'], sourceUnchangedEvidence: 'database.json / DB-08'};
fs.writeFileSync(path.join(dir, 'acceptance-summary.json'), JSON.stringify(result, null, 2));
console.log(JSON.stringify({runId: result.runId, status: result.status, checks: result.checks, phases: result.phases}));
