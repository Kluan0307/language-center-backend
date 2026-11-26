const fs = require('node:fs'), assert = require('node:assert/strict');
const file = 'D:/hoctap/KLTN/language-center/language-center-web-frontend/src/pages/CourseManagement/index.tsx';
let text = fs.readFileSync(file, 'utf8');
for (const [before, after] of [
  ['classification.data?.programs.find(p=>p.id===c.programId)?.name||c.type||"—"', 'classification.data?.programs.find(p=>p.id===c.programId)?.name||"Chưa xác nhận chương trình"'],
  ['classification.data?.programs.find(p=>p.id===detail.programId)?.name||detail.type||"—"', 'classification.data?.programs.find(p=>p.id===detail.programId)?.name||"Chưa xác nhận chương trình"'],
  ['label: "Loại khóa",', 'label: "Chương trình",'],
]) { assert.equal(text.split(before).length, 2, before); text = text.replace(before, after); }
fs.writeFileSync(file, text);
console.log('Updated course classification display in CMS.');
