const fs=require('node:fs'),assert=require('node:assert/strict');
const file='D:/hoctap/KLTN/language-center/language-center-web-frontend/tests/navigation.test.mjs';
let text=fs.readFileSync(file,'utf8');
const before="[...getNavigation(role).map((item) => item.path), '/company', '/help', '/settings'].sort()";
const after="[...getNavigation(role).map((item) => item.path), ...(role === 'SUPER_ADMIN' || role === 'ADMIN' ? ['/website-content'] : []), '/company', '/help', '/settings'].sort()";
assert.equal(text.split(before).length,2);fs.writeFileSync(file,text.replace(before,after));
console.log('Updated existing sidebar regression expectation for manager-only website CMS link.');
