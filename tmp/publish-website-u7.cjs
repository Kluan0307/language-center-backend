const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),docs=path.join(root,'docs/course-classification'),{query}=require('../scripts/identity/database.cjs');
const reports={};
for(const browser of ['chrome','edge']){
 const report=JSON.parse(fs.readFileSync(path.join(docs,'u7-real-'+browser+'-validation.json'),'utf8'));assert.equal(report.status,'passed');assert.equal(report.sourceUnchanged,true);assert.equal(report.sourceHash,report.sourceAfterHash);
 const database=path.basename(report.artifacts);assert.match(database,/^lc_identity_http_\d+$/);assert.equal(Number(query(null,"SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='"+database+"'")),0,'Scratch schema not removed');
 const source=path.join(root,report.artifacts,'browser'),dest=path.join(docs,'website-ui-u7-after',browser);fs.mkdirSync(dest,{recursive:true});
 const details=JSON.parse(fs.readFileSync(path.join(source,'u7-ui-details.json'),'utf8'));assert.equal(details.pages.length,96);assert.equal(details.actualZoom.length,4);
 fs.copyFileSync(path.join(source,'u7-ui-details.json'),path.join(dest,'measurements.json'));
 for(const image of fs.readdirSync(source).filter(x=>x.endsWith('.png')&&!x.startsWith('failure')))fs.copyFileSync(path.join(source,image),path.join(dest,image));
 reports[browser]={apiChecks:report.checks.length,browserChecks:report.browser.checks.length,pages:details.pages.length,actualZoomPages:details.actualZoom.length,sourceUnchanged:true,scratchDatabaseRemoved:true};
}
const result={checkedAt:new Date().toISOString(),status:'passed',scope:'Website U1-U7 local automated acceptance',reports,build:true,typecheck:true,lint:true,physicalPhone:false,safariFirefox:false};
fs.writeFileSync(path.join(docs,'u7-acceptance.json'),JSON.stringify(result,null,2));
const roadmap=path.join(docs,'website-ui-roadmap.md');let plan=fs.readFileSync(roadmap,'utf8');plan=plan.replace('U1–U6 hoàn thành; U7 chưa thực hiện','U1–U7 hoàn thành phần local tự động; kiểm tra điện thoại thật khi có thiết bị');plan=plan.replace('Chưa nghiệm thu lại MySQL thật/thiết bị thật ở U7.','U7 đã nghiệm thu bằng service thật/MySQL bản sao và Chrome/Edge; điện thoại thật còn cần kiểm tra khi có thiết bị.');if(!plan.includes('[Bàn giao U7'))plan=plan.replace('## Cơ sở kiểm tra','[Bàn giao U7, báo cáo Chrome/Edge và giới hạn kiểm chứng](u7-handoff.md).\n\n## Cơ sở kiểm tra');fs.writeFileSync(roadmap,plan);
const summary='**Website U7 (20/11/2026):** hoàn tất nghiệm thu local với 5 service thật/MySQL bản sao, Chrome và Edge; zoom thật 200%, bàn phím và focus form đã kiểm tra. Build/TypeScript/lint đạt; nguồn DB không đổi, database tạm đã dọn. Điện thoại thật/Safari/Firefox chưa kiểm thử. ';
for(const [file,link] of [[path.join(root,'README.md'),'docs/course-classification/u7-handoff.md'],['D:/hoctap/KLTN/language-center/language-center-website/README.md','../language-center-backend/language-center-backend/docs/course-classification/u7-handoff.md']]){const old=fs.readFileSync(file,'utf8');if(!old.startsWith('**Website U7'))fs.writeFileSync(file,summary+'[Bàn giao và kết quả]('+link+').\n\n'+old);}
console.log(JSON.stringify(result,null,2));
