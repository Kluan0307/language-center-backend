const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const services = [
  { name: 'auth', dir: 'auth-service' },
  { name: 'academic', dir: 'academic-service' },
  { name: 'teaching', dir: 'teaching-service' },
  { name: 'finance', dir: 'finance-service' },
  { name: 'assistant', dir: 'assistant-service' }
];

function walk(dir) {
  let results = [];
  const list = fs.readdirSync(dir);
  list.forEach(file => {
    file = path.join(dir, file);
    const stat = fs.statSync(file);
    if (stat && stat.isDirectory()) results = results.concat(walk(file));
    else results.push(file);
  });
  return results;
}

const inventory = [];

services.forEach(svc => {
  const javaDir = path.join(root, svc.dir, 'src/main/java');
  const files = walk(javaDir).filter(f => f.endsWith('Controller.java'));
  files.forEach(f => {
    const content = fs.readFileSync(f, 'utf8');
    const classMatch = content.match(/public\s+class\s+(\w+)/);
    if (!classMatch) return;
    const className = classMatch[1];
    const classHeader = content.substring(0, classMatch.index);
    const classBody = content.substring(content.indexOf('{', classMatch.index) + 1);

    // Class-level RequestMapping
    const baseMatch = classHeader.match(/@RequestMapping\((?:(?:value|path)\s*=\s*)?["']([^"']*)["']/);
    const basePath = baseMatch ? baseMatch[1] : '';

    const classPreAuthMatch = classHeader.match(/@PreAuthorize\(\"([^\"]*)\"\)/);
    const classPreAuth = classPreAuthMatch ? classPreAuthMatch[1] : '';

    // Regex to match each mapped method
    // Finds Mapping annotation and the public method following it
    const methodRegex = /@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping|RequestMapping)(?:\s*\((.*?)\))?([\s\S]*?public\s+[\w\<\>\[\],\s\.\?]+\s+(\w+)\s*\(([^)]*)\))/g;

    let m;
    while ((m = methodRegex.exec(classBody)) !== null) {
      const mappingType = m[1];
      const mappingArgs = m[2] ? m[2].trim() : '';
      const betweenBlock = m[3];
      const handlerName = m[4];
      const paramsBlock = m[5];

      // HTTP Method
      let httpMethod = 'GET';
      if (mappingType === 'PostMapping') httpMethod = 'POST';
      else if (mappingType === 'PutMapping') httpMethod = 'PUT';
      else if (mappingType === 'PatchMapping') httpMethod = 'PATCH';
      else if (mappingType === 'DeleteMapping') httpMethod = 'DELETE';
      else if (mappingType === 'RequestMapping') {
        const reqMethodMatch = mappingArgs.match(/method\s*=\s*(?:RequestMethod\.)?(\w+)/);
        if (reqMethodMatch) httpMethod = reqMethodMatch[1];
      }

      // PreAuthorize
      const methodPreAuthMatch = betweenBlock.match(/@PreAuthorize\(\"([^\"]*)\"\)/);
      let preAuth = methodPreAuthMatch ? methodPreAuthMatch[1] : classPreAuth;

      // Extract paths from mappingArgs
      const stringLiterals = [...mappingArgs.matchAll(/["']([^"']*)["']/g)].map(x => x[1]);
      const pathLiterals = stringLiterals.filter(s => !['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].includes(s));
      const paths = pathLiterals.length > 0 ? pathLiterals : [''];

      paths.forEach((p, idx) => {
        let fullPath = (basePath + (p.startsWith('/') ? p : (p ? '/' + p : ''))).replace(/\/+/g, '/');
        if (fullPath.endsWith('/') && fullPath.length > 1) fullPath = fullPath.slice(0, -1);
        if (!fullPath) fullPath = '/';

        const isAlias = idx > 0;
        const isDiagnostic = className === 'HomeController' && fullPath === '/';

        inventory.push({
          service: svc.name,
          controller: className,
          method: httpMethod,
          path: fullPath,
          handler: handlerName,
          isAlias,
          isDiagnostic,
          preAuth: preAuth || 'PermitAll/Inherited'
        });
      });
    }
  });
});

const primaryEndpoints = inventory.filter(e => !e.isAlias && !e.isDiagnostic);
const aliases = inventory.filter(e => e.isAlias);
const diagnostics = inventory.filter(e => e.isDiagnostic);

console.log('--- KẾT QUẢ KIỂM KÊ API THỰC TẾ ---');
console.log('Tổng số endpoint ghi nhận:', inventory.length);
console.log('Số API nghiệp vụ chính (Primary):', primaryEndpoints.length);
console.log('Số alias phụ:', aliases.length);
console.log('Số endpoint chẩn đoán (Diagnostic):', diagnostics.length);

const byService = {};
primaryEndpoints.forEach(e => {
  byService[e.service] = (byService[e.service] || 0) + 1;
});
console.log('API chính theo Service:', byService);

fs.writeFileSync(path.join(root, 'storage/api-inventory.json'), JSON.stringify({
  summary: {
    total: inventory.length,
    primary: primaryEndpoints.length,
    aliases: aliases.length,
    diagnostics: diagnostics.length,
    byService
  },
  primaryEndpoints,
  aliases,
  diagnostics
}, null, 2));
