const fs=require('node:fs');
const file='D:/hoctap/KLTN/language-center/language-center-website/src/app/pages.css';
let css=fs.readFileSync(file,'utf8');
if(!css.includes('scroll-margin-block:'))css=css.replace('.consultationForm textarea {', '.consultationForm :is(input, select, textarea, button) { scroll-margin-block: calc(var(--header-height) + 16px) 24px; }\n.consultationForm textarea {');
fs.writeFileSync(file,css);
