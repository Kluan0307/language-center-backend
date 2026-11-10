// Static source/contract/evidence checks for the G15/G16/G19 revision, not runtime testing.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const root = path.resolve(__dirname, '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8').replace(/^\uFEFF/, '');
const json = p => JSON.parse(read(p));
const hash = p => crypto.createHash('sha256').update(fs.readFileSync(path.join(root, p))).digest('hex');
let checks = 0;
const check = (condition, message) => { assert.ok(condition, message); checks++; };
execFileSync(process.execPath, [path.join(root, 'scripts/inventory-api.cjs')], {cwd: root, stdio: 'pipe'});
const inventory = json('storage/api-inventory.json');
check(inventory.summary.primary === 173, 'Primary API inventory changed; review counts');
const normalize = p => p.split('?')[0].replace(/\{[^}]+\}/g, '{}');
const routes = new Set([...inventory.primaryEndpoints, ...inventory.aliases, ...inventory.diagnostics].map(e => e.method + ' ' + normalize(e.path)));
const diagramDir = 'docs/thesis/diagrams';
const dynamicFiles = fs.readdirSync(path.join(root, diagramDir)).filter(f => /^(activity|sequence)-.*\.puml$/.test(f)).map(f => diagramDir + '/' + f);
const reportFiles = fs.readdirSync(path.join(root, 'docs/thesis/report')).filter(f => f.endsWith('.md')).map(f => 'docs/thesis/report/' + f);
for (const file of [...dynamicFiles, ...reportFiles, 'docs/thesis/traceability.md', 'docs/thesis/design/dynamic-workflows.md']) {
  const text = read(file);
  check(!text.includes('\uFFFD'), 'Damaged UTF-8: ' + file);
  for (const m of text.matchAll(/\b(GET|POST|PUT|PATCH|DELETE)\s+(\/api\/v1\/[A-Za-z0-9_{}\/-]+)/g))
    check(routes.has(m[1] + ' ' + normalize(m[2])), 'Unknown documented route: ' + file + ' ' + m[0]);
}
const reviewed = json('docs/thesis/design/dynamic-source-review.json');
for (const source of reviewed.sources) check(hash(source.file) === source.sha256, 'Reviewed source changed: ' + source.file);
const manifest = json('docs/reviews/2026-11-10-g17-g18-evidence.json');
check(manifest.status === 'PASSED' && manifest.checks === 86, 'Acceptance evidence missing');
check(manifest.phases.reduce((n, p) => n + p.checks, 0) === 86, 'Acceptance phases double-counted');
const runBase = 'storage/acceptance/' + manifest.runId;
const rendered = json(diagramDir + '/render-manifest.json');
for (const file of dynamicFiles) {
  const name = path.basename(file), entry = rendered.diagrams.find(e => e.source === name);
  check(entry && entry.sourceSha256 === hash(file), 'Stale PlantUML hash: ' + file);
  check(entry.svgSha256 === hash(file.replace('.puml', '.svg')), 'Stale SVG hash: ' + file);
}
for (const name of ['g17-student-profile', 'g17-class-closure', 'g17-mobile-profile', 'g18-branch-switch']) {
  const source = runBase + '/' + name + '.png';
  const entry = manifest.inventory.find(e => e.file === name + '.png');
  check(entry && hash(source) === entry.sha256, 'Original UI evidence changed: ' + name);
  check(hash('docs/thesis/report/figures/' + name + '.png') === entry.sha256, 'Report image differs from real browser evidence: ' + name);
}
const trace = read('docs/thesis/traceability.md');
for (let n = 1; n <= 12; n++) check(trace.includes('|RQ' + String(n).padStart(2, '0') + '|'), 'Missing RQ row ' + n);
const slides = read('docs/thesis/report/slide-va-cau-hoi-bao-ve.md');
check([...slides.matchAll(/^\d+\. /gm)].length === 18, 'Expected 18 slide topics');
check([...slides.matchAll(/^### Câu\d+:/gm)].length === 20, 'Expected 20 defense questions');
const report = {passed: true, checks, primaryApis: inventory.summary.primary, sourceFiles: reviewed.sources.length, dynamicDiagrams: dynamicFiles.length, reportFiles: reportFiles.length, browserFigures: 4, acceptanceChecksReferenced: 86, scope: 'Static contract/source/render/evidence consistency; not a new business, usability, load or production test'};
fs.writeFileSync(path.join(root, 'storage/g15-g16-g19-revision-check.json'), JSON.stringify(report, null, 2));
console.log(JSON.stringify(report));
