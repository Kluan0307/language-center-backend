const fs=require('node:fs'),assert=require('node:assert/strict');
const file='scripts/website/verify-l67.cjs';let text=fs.readFileSync(file,'utf8');
assert.equal(text.split('.desktopNav a[').length,3);
text=text.replaceAll('.desktopNav a[','.headerUtility a[');fs.writeFileSync(file,text);
console.log('Updated existing website regression selectors for the new utility navigation.');
