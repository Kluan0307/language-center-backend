const fs = require('node:fs'), path = require('node:path'), crypto = require('node:crypto');
function verify(directory) {
  const root = fs.realpathSync(directory), manifest = JSON.parse(fs.readFileSync(path.join(root, 'release.json'), 'utf8'));
  if (!Array.isArray(manifest.files) || !manifest.files.length) throw Error('Release manifest missing');
  const names = new Set();
  for (const entry of manifest.files) {
    if (typeof entry.path !== 'string' || entry.path.includes('\\') || path.isAbsolute(entry.path) || entry.path.split('/').some(p => !p || p === '..' || p === '.') || names.has(entry.path)) throw Error('Unsafe/duplicate manifest path');
    names.add(entry.path);
    const file = path.resolve(root, entry.path), stat = fs.lstatSync(file);
    if (!file.startsWith(root + path.sep) || !stat.isFile() || stat.isSymbolicLink() || !fs.realpathSync(file).startsWith(root + path.sep)) throw Error('Release file outside package');
    if (stat.size !== entry.bytes || crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex') !== entry.sha256) throw Error('Release checksum mismatch: ' + entry.path);
  }
  function walk(folder) {
    for (const entry of fs.readdirSync(folder, {withFileTypes: true})) {
      const file = path.join(folder, entry.name), relative = path.relative(root, file).replaceAll('\\', '/');
      if (entry.isSymbolicLink()) throw Error('Release contains symlink');
      if (entry.isDirectory()) walk(file);
      else if (relative !== 'release.json' && !names.has(relative)) throw Error('Unmanifested file: ' + relative);
    }
  }
  walk(root);
  return {release: root, verified: true, artifacts: names.size, dirtyWorkingTree: manifest.dirtyWorkingTree};
}
module.exports = {verify};
if (require.main === module) { try { console.log(JSON.stringify(verify(process.argv[2]))); } catch (e) {console.error(e.message); process.exitCode = 1;} }
