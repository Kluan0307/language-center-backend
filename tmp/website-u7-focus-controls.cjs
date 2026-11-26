const fs=require('node:fs');
const file='D:/hoctap/KLTN/language-center/language-center-website/src/app/pages.css';
fs.writeFileSync(file,fs.readFileSync(file,'utf8').replace('.consultationForm :is(input, select, textarea, button)', '.consultationForm :is(input, select, textarea)'));
