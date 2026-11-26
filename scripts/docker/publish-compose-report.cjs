const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../..'),docs=path.join(root,'docs/docker');
const integration=JSON.parse(fs.readFileSync(path.join(docs,'dck3-validation.json'),'utf8')),main=JSON.parse(fs.readFileSync(path.join(docs,'dck3-main-status.json'),'utf8'));
assert.equal(integration.status,'passed');assert.equal(integration.temporaryProjectRemoved,true);assert.equal(main.status,'passed');assert.equal(main.wrapperMatches,true);assert.equal(main.services.length,7);
const result={checkedAt:new Date().toISOString(),stage:'DCK3',status:'complete',checks:integration.checks.length,services:7,gateway:main.gateway,tableCount:main.tableCount,rowsImported:0,sourceDatabaseWritten:false,temporaryProjectRemoved:true,productionDataMigration:'DCK5',websiteIntegration:'DCK4'};
fs.writeFileSync(path.join(docs,'dck3-completion.json'),JSON.stringify(result,null,2));
const readme=path.join(root,'README.md'),summary='**Docker DCK3 (24/11/2026):** Compose backend + MySQL đã chạy; 7 container healthy tại Gateway http://localhost:18080. Có 87 bảng rỗng, tách network/secret, volume DB/tài liệu và profile Assistant. 18 kiểm tra tích hợp đạt, đã thử tạo lại container và giữ dữ liệu/file. Chưa nhập dữ liệu nguồn; DCK4 ghép web, DCK5 restore dữ liệu. [Hướng dẫn chạy/dừng và báo cáo](docs/docker/dck3-handoff.md).\n\n';
const old=fs.readFileSync(readme,'utf8');if(!old.startsWith('**Docker DCK3'))fs.writeFileSync(readme,summary+old);
console.log(JSON.stringify(result));
