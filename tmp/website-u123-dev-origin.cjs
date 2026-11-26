const fs=require('node:fs');
const file='D:/hoctap/KLTN/language-center/language-center-website/next.config.ts';
let source=fs.readFileSync(file,'utf8');
if(!source.includes('allowedDevOrigins'))source=source.replace('poweredByHeader:false,','poweredByHeader:false,allowedDevOrigins:["127.0.0.1"],');
fs.writeFileSync(file,source);
