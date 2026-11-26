const c=require('./data-common.cjs'),{fs,path,root,assert}=c,crypto=require('crypto'),{spawn}=require('child_process');
const dir=path.join(root,'storage/sample-learning-media'),state=JSON.parse(fs.readFileSync(path.join(dir,'import.json'),'utf8'));
const checks=[];const request=(url,options={})=>fetch(url,{...options,signal:AbortSignal.timeout(30000)});
const base='http://127.0.0.1:18080/api/v1/';
async function login(username){const r=await request(base+'auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username,password:process.env.LC_SEED_PASSWORD||'123456'})});assert.equal(r.status,200,'Demo login failed');return (await r.json()).token;}
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
let browser,socket,seq=0;const pending=new Map(),delay=ms=>new Promise(r=>setTimeout(r,ms));
function send(method,params={}){return new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});socket.send(JSON.stringify({id,method,params}));});}
async function evaluate(expression){const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.text);return r.result.value;}
async function main(){
 const photos=new Set(Object.values(state.images).map(x=>x.url));
 for(const kind of ['PROGRAM','COURSE']){const r=await request(base+'academic/public/catalog/'+kind+'?size=100');assert.equal(r.status,200);const rows=(await r.json()).items;const expected=kind==='PROGRAM'?4:20;assert.equal(rows.filter(x=>photos.has(x.imageUrl)).length,expected);assert.ok(rows.every(x=>!photos.has(x.imageUrl)||x.summary.includes('Ảnh minh họa')));checks.push('Public catalog '+kind+': '+expected+' images and illustration labels');}
 // One enrolled student per seeded course and branch. Only demo usernames are read.
 const rows=c.mysql("SELECT a.username,cl.branch_id,cl.course_id,MIN(e.id) FROM students s JOIN accounts a ON a.id=s.account_id JOIN enrollments e ON e.student_id=s.id JOIN school_classes cl ON cl.id=e.class_id WHERE e.study_status='STUDYING' AND cl.course_id IN (17,19,21,23,26,30,22) AND a.status=1 GROUP BY a.username,cl.branch_id,cl.course_id ORDER BY cl.branch_id,cl.course_id").trim().split(/\r?\n/).filter(Boolean).map(x=>x.split('\t'));
 const selected=new Map();for(const row of rows){const key=row[1]+'/'+row[2];if(!selected.has(key))selected.set(key,row);}
 assert.equal(selected.size,28,'Expected one eligible demo student for each seeded course/branch');
 for(const [username,branch,course,enrollment] of selected.values()){
 const token=await login(username),headers={Authorization:'Bearer '+token};
 const r=await request(base+'teaching/student/me/documents?enrollmentId='+enrollment+'&size=100',{headers});assert.equal(r.status,200);const docs=(await r.json()).content;
 const expected=Object.values(state.documents).find(d=>d.branchId===Number(branch)&&d.courseId===Number(course));assert.ok(expected);assert.ok(docs.some(d=>d.id===expected.id),'Expected shared PDF missing');
 const file=await request(base+'teaching/student/me/documents/'+expected.id+'/download',{headers});assert.equal(file.status,200);assert.equal(sha(Buffer.from(await file.arrayBuffer())),sha(fs.readFileSync(path.join(dir,'documents',expected.filename))));
 const foreign=Object.values(state.documents).find(d=>d.branchId!==Number(branch)&&d.courseId===Number(course));const denied=await request(base+'teaching/student/me/documents/'+foreign.id+'/download',{headers});assert.equal(denied.status,404,'Foreign branch file exposed');
 }
 checks.push('28 course/branch student lists and PDF downloads passed');checks.push('28 foreign-branch downloads denied with HTTP 404');
 const unauth=await request(base+'teaching/student/me/documents/'+Object.values(state.documents)[0].id+'/download');assert.equal(unauth.status,401);checks.push('Anonymous document download denied');
 browser=spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=19254','--user-data-dir='+path.join(dir,'chrome-profile'),'about:blank'],{windowsHide:true,stdio:'ignore'});
 let target;for(let i=0;i<100;i++){try{target=(await(await request('http://127.0.0.1:19254/json/list')).json()).find(t=>t.type==='page');if(target)break;}catch{}await delay(100);}assert.ok(target,'Chrome not ready');socket=new WebSocket(target.webSocketDebuggerUrl);await new Promise(r=>socket.addEventListener('open',r,{once:true}));socket.addEventListener('message',e=>{const m=JSON.parse(e.data);if(m.id&&pending.has(m.id)){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(Error(m.error.message)):p.resolve(m.result);}});
 await send('Page.enable');const shots=path.join(root,'docs/docker/screenshots');fs.mkdirSync(shots,{recursive:true});
 for(const width of [1440,390]){await send('Emulation.setDeviceMetricsOverride',{width,height:900,deviceScaleFactor:1,mobile:width<500});await send('Page.navigate',{url:'http://localhost:13000/khoa-hoc/mg1'});let ready=false;for(let i=0;i<100;i++){ready=await evaluate('document.readyState==="complete" && [...document.images].some(i=>i.src.includes("res.cloudinary.com") && i.complete && i.naturalWidth>0)');if(ready)break;await delay(100);}assert.ok(ready,'Public course image not rendered');assert.ok(await evaluate('document.body.innerText.includes("Ảnh minh họa học tập")'));const shot=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.join(shots,'learning-media-'+width+'.png'),Buffer.from(shot.data,'base64'));checks.push('Chrome course page '+width+'px: real Cloudinary image rendered');}
 const backup=JSON.parse(fs.readFileSync(path.join(root,'storage/docker-backups',state.backupId,'backup.json'),'utf8'));
 const allowed=new Set(['accounts','document_events','document_sharing_events','documents','media_assets','student_notification_outbox','website_edit_events','website_publications']);
 const changed=c.dockerSnapshot().tables.filter(t=>backup.snapshot.tables.find(o=>o.name===t.name)?.sha256!==t.sha256).map(t=>t.name);assert.ok(changed.every(t=>allowed.has(t)),'Unexpected business table modified: '+changed.filter(t=>!allowed.has(t)).join(', '));checks.push('Backup comparison: no unrelated business tables modified');
 const report=JSON.parse(fs.readFileSync(path.join(root,'docs/docker/sample-learning-media.json'),'utf8'));report.checks=[...new Set([...report.checks,...checks])];report.studentAccessVerified=true;report.changedTables=changed;fs.writeFileSync(path.join(root,'docs/docker/sample-learning-media.json'),JSON.stringify(report,null,2));console.log(JSON.stringify({status:'passed',checks}));
}
main().catch(e=>{console.error(e.message);process.exitCode=1;}).finally(async()=>{if(socket?.readyState===WebSocket.OPEN){try{await Promise.race([send('Browser.close'),delay(1000)]);}catch{}socket.close();}if(browser&&!browser.killed)browser.kill();});
