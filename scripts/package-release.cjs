// Assemble already verified artifacts. Does not run builds, publish, or include .env files.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {execFileSync} = require('node:child_process');
const root = path.resolve(__dirname, '..');
const frontend = path.resolve(process.argv[2] || path.join(root, '../../language-center-web-frontend'));
const output = path.resolve(process.argv[3] || path.join(root, 'storage/releases/release-' + Date.now()));
function copy(source, relative) {
  const stat = fs.lstatSync(source);
  if (stat.isSymbolicLink()) throw Error('Release must not contain links');
  const target = path.join(output, relative);
  if (stat.isDirectory()) {
    fs.mkdirSync(target, {recursive: true});
    for (const entry of fs.readdirSync(source)) copy(path.join(source, entry), path.join(relative, entry));
  } else {
    fs.mkdirSync(path.dirname(target), {recursive: true});
    fs.copyFileSync(source, target, fs.constants.COPYFILE_EXCL);
  }
}
try {
  if (fs.existsSync(output)) throw Error('Release directory already exists');
  const jars = ['auth-service', 'academic-service', 'teaching-service', 'finance-service', 'api-gateway'].map(service => {
    const files = fs.readdirSync(path.join(root, service, 'target')).filter(f => f.endsWith('.jar'));
    if (files.length !== 1) throw Error('Exactly one packaged JAR required for ' + service);
    return [service, path.join(root, service, 'target', files[0])];
  });
  const html = path.join(frontend, 'dist/index.html');
  if (!fs.existsSync(html)) throw Error('Build frontend with VITE_API_BASE_URL=/api/v1 first');
  const assets = fs.readdirSync(path.join(frontend, 'dist/assets')).filter(f => f.endsWith('.js')).map(f => fs.readFileSync(path.join(frontend, 'dist/assets', f), 'utf8')).join('\n');
  if (assets.includes('http://localhost:8080/api/v1')) throw Error('Frontend still contains developer API URL; rebuild with VITE_API_BASE_URL=/api/v1');
  fs.mkdirSync(output, {recursive: true});
  for (const [service, jar] of jars) copy(jar, 'backend/' + service + '.jar');
  copy(path.join(frontend, 'dist'), 'frontend');
  for (const file of ['nginx.conf', 'production.env.example', 'release-handoff.md'])
    copy(path.join(root, 'deployment', file), 'deployment/' + file);
  copy(path.join(root, 'docs/migrations'), 'docs/migrations');
  copy(path.join(root, 'docs/improvements/phase-seven-deployment.md'), 'docs/improvements/phase-seven-deployment.md');
  for (const file of ['start-release.ps1', 'operations.cjs', 'verify-release.cjs', 'serve-release.cjs']) copy(path.join(root, 'scripts', file), 'scripts/' + file);
  // Preserve the workspace layout so relative links and FE/BE source relationships survive export.
  const sourceRoot = 'source/language-center';
  const backendSource = sourceRoot + '/language-center-backend/language-center-backend';
  for (const [service] of jars) {
    copy(path.join(root, service, 'src'), backendSource + '/' + service + '/src');
    copy(path.join(root, service, 'pom.xml'), backendSource + '/' + service + '/pom.xml');
  }
  for (const file of ['pom.xml', 'README.md', '.env.example']) copy(path.join(root, file), backendSource + '/' + file);
  for (const folder of ['docs', 'scripts', 'deployment']) copy(path.join(root, folder), backendSource + '/' + folder);
  const frontendSource = sourceRoot + '/language-center-web-frontend';
  for (const folder of ['src', 'public', 'tests', 'docs']) if (fs.existsSync(path.join(frontend, folder))) copy(path.join(frontend, folder), frontendSource + '/' + folder);
  for (const file of ['.env.example', '.gitignore']) if (fs.existsSync(path.join(frontend, file))) copy(path.join(frontend, file), frontendSource + '/' + file);
  for (const file of fs.readdirSync(frontend).filter(f => /^(package(?:-lock)?\.json|tsconfig.*\.json|vite\.config\.ts|eslint\.config\.js|index\.html|README\.md)$/.test(f))) copy(path.join(frontend, file), frontendSource + '/' + file);
  for (const file of ['README.md', 'KE_HOACH_HOAN_THIEN.md']) copy(path.resolve(root, '../..', file), sourceRoot + '/' + file);
  copy(path.join(root, 'deployment/release-handoff.md'), 'HANDOFF.md');
  const files = [];
  function inventory(directory) {
    for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
      const file = path.join(directory, entry.name);
      if (entry.isDirectory()) inventory(file);
      else files.push({path: path.relative(output, file).replaceAll('\\', '/'), bytes: fs.statSync(file).size, sha256: crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')});
    }
  }
  inventory(output);
  let revision = 'unknown', dirty = true;
  try { revision = execFileSync('git', ['rev-parse', 'HEAD'], {cwd: root, encoding: 'utf8', windowsHide: true}).trim(); dirty = Boolean(execFileSync('git', ['status', '--porcelain'], {cwd: root, encoding: 'utf8', windowsHide: true}).trim()); } catch {}
  fs.writeFileSync(path.join(output, 'release.json'), JSON.stringify({createdAt: new Date().toISOString(), revision, dirtyWorkingTree: dirty, files}, null, 2));
  console.log(JSON.stringify({release: output, artifacts: files.length, dirtyWorkingTree: dirty}));
} catch (error) { console.error(error.message); process.exitCode = 1; }
