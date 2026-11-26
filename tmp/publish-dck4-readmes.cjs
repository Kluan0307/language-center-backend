const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..'),workspace=path.resolve(root,'../..');
const docs='language-center-backend/language-center-backend/docs/docker/dck4-handoff.md';
for(const [dir,label,url] of [['language-center-web-frontend','Web quản lý','http://localhost:15173'],['language-center-website','Website công khai','http://localhost:13000']]){
 const file=path.join(workspace,dir,'README.md'),old=fs.readFileSync(file,'utf8');
 const note=`**Docker DCK4 (24/11/2026):** ${label} chạy tại ${url}, cùng stack Gateway/MySQL riêng. Từ backend root chạy \`.\\scripts\\docker\\compose.ps1 -Action up -Web -Assistant -Build\` (các lần sau bỏ \`-Build\`). Database Docker hiện rỗng; dữ liệu/tài khoản cũ chờ phục hồi ở DCK5. [Hướng dẫn cấu hình và kiểm chứng](../${docs}).\n\n`;
 if(!old.includes('**Docker DCK4 (24/11/2026):**'))fs.writeFileSync(file,note+old);
}
const outside=path.join(workspace,'README.md');
if(fs.existsSync(outside)){
 const old=fs.readFileSync(outside,'utf8');if(!old.includes('**Docker DCK4 (24/11/2026):**'))fs.writeFileSync(outside,'**Docker DCK4 (24/11/2026):** 9 container healthy; web quản lý http://localhost:15173, website http://localhost:13000, Gateway http://localhost:18080. MySQL Docker riêng vẫn rỗng; DCK5 mới restore dữ liệu. [Cách chạy và kết quả](./'+docs+').\n\n'+old);
}
console.log('DCK4 handoff linked from workspace and both web READMEs.');
