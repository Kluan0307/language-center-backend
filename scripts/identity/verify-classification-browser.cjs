// CMS browser acceptance against a private HTTP fixture; never calls application databases.
const fs = require('node:fs'), path = require('node:path'), http = require('node:http');
const assert = require('node:assert/strict'), {spawn, execFileSync} = require('node:child_process');
const frontend = path.resolve(__dirname, '../../../../language-center-web-frontend');
const out = path.resolve(__dirname, '../../tmp/classification-g8-cms');
fs.mkdirSync(out, {recursive:true});
const base = 'http://127.0.0.1:19390', checks = [], requests = [], children = [];
let ws, server, role = 'SUPER_ADMIN', seq = 0;
const pending = new Map(), exceptions = [], delay = ms => new Promise(r => setTimeout(r,ms));
const programs = ['GE','COMMUNICATION','IELTS','TOEIC'].map((code,i)=>({id:i+1,code,name:code,active:true}));
const audiences = ['PRESCHOOL','PRIMARY','SECONDARY','HIGH_SCHOOL','ADULT'].map((code,i)=>({id:i+1,code,name:['Mẫu giáo','Tiểu học','THCS','THPT','Người lớn'][i],active:true}));
const course = {id:1,code:'G8KH',name:'IELTS Foundation',type:'LEGACY_NOT_A_PROGRAM',level:null,duration:null,expectedMinutes:1200,tuitionFee:4000000,maxStudents:20,status:'OPEN',branchId:1,programId:3,audiences:[audiences[2],audiences[4]],classificationRevision:1,classificationComplete:true};
const legacy = {...course,id:2,code:'G8OLD',name:'Khóa chưa phân loại',programId:null,audiences:[],classificationRevision:0,classificationComplete:false};
const page = content => ({content,page:0,size:20,totalElements:content.length,totalPages:1});
function command(method,params={}) {
  return new Promise((resolve,reject)=>{const id=++seq;
    const timeout=setTimeout(()=>{pending.delete(id);reject(Error(method+' timeout'));},15000);
    pending.set(id,{resolve,reject,timeout});ws.send(JSON.stringify({id,method,params}));});
}
async function evaluate(expression) {
  const r=await command('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});
  if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);
  return r.result.value;
}
async function wait(expression) {for(let i=0;i<100;i++){if(await evaluate(expression))return;await delay(100);}throw Error('Timeout: '+expression);}
async function check(name,expression) {assert.ok(await evaluate(expression),name);checks.push(name);}
async function click(selector) {
  const point=await evaluate(`(()=>{const e=document.querySelector(${JSON.stringify(selector)});if(!e)throw Error('Missing click target');const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2};})()`);
  await command('Input.dispatchMouseEvent',{type:'mousePressed',...point,button:'left',clickCount:1});
  await command('Input.dispatchMouseEvent',{type:'mouseReleased',...point,button:'left',clickCount:1});
}
async function visit(route='/courses') {await command('Page.navigate',{url:base+route});await wait('document.body?.innerText.includes("IELTS Foundation")');}
function launch(exe,args,env=process.env,cwd=process.cwd()) {
  const log=fs.openSync(path.join(out,'process-'+children.length+'.log'),'w');
  const child=spawn(exe,args,{cwd,env,windowsHide:true,stdio:['ignore',log,log]});children.push(child);fs.closeSync(log);return child;
}
async function run() {
  server=http.createServer((req,res)=>{
    res.setHeader('Access-Control-Allow-Origin',base);
    res.setHeader('Access-Control-Allow-Headers','Authorization,Content-Type,X-Branch-Id');
    if(req.method==='OPTIONS'){res.writeHead(204);return res.end();}
    requests.push(req.url);const u=new URL(req.url,'http://fixture');let data={};
    if(u.pathname.endsWith('/auth/me')) data={accountId:1,profileId:1,branchId:1,branchIds:[1],username:'acceptance',fullName:'Nghiệm thu',role,status:true,mustChangePassword:false};
    else if(u.pathname.endsWith('/classification-catalog'))data={programs,audiences};
    else if(u.pathname.endsWith('/branches'))data=page([{id:1,code:'G8CN',name:'Chi nhánh 1',status:'ACTIVE'}]);
    else if(u.pathname.endsWith('/courses'))data=page([course,legacy].filter(c=>(!u.searchParams.get('programId')||c.programId===Number(u.searchParams.get('programId')))&&(!u.searchParams.get('audienceId')||c.audiences.some(a=>a.id===Number(u.searchParams.get('audienceId'))))));
    else if(u.pathname.endsWith('/courses/1'))data=course;
    else if(u.pathname.endsWith('/history')||u.pathname.endsWith('/duration-history'))data=[];
    else if(u.pathname.endsWith('/duration-plan'))data={courseId:1,revision:0,expectedMinutes:1200,legacyDuration:null,verified:true,canManage:false};
    else if(u.pathname.endsWith('/admission'))data={policy:{courseId:1,revision:0,mode:'UNVERIFIED',acceptedLevels:[]},canManage:false};
    else if(u.pathname.includes('assistant')){res.writeHead(503,{'Content-Type':'application/json'});return res.end('{}');}
    res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify(data));
  });await new Promise((r,j)=>{server.once('error',j);server.listen(19380,'127.0.0.1',r);});
  launch(process.execPath,[path.join(frontend,'node_modules/vite/bin/vite.js'),'--host','127.0.0.1','--port','19390','--strictPort'],{...process.env,VITE_API_BASE_URL:'http://127.0.0.1:19380/api/v1'},frontend);
  launch('C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--remote-debugging-port=19327','--user-data-dir='+path.join(out,'chrome-profile'),'about:blank']);
  let target;
  for(let i=0;i<100;i++){try{target=(await(await fetch('http://127.0.0.1:19327/json/list')).json()).find(p=>p.type==='page');if(target&&(await fetch(base)).ok)break;}catch{}await delay(150);}
  assert.ok(target,'Browser startup');ws=new WebSocket(target.webSocketDebuggerUrl);
  ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.method==='Runtime.exceptionThrown')exceptions.push(m.params.exceptionDetails.text);if(m.id&&pending.has(m.id)){const p=pending.get(m.id);pending.delete(m.id);clearTimeout(p.timeout);m.error?p.reject(Error(m.error.message)):p.resolve(m.result);}};
  await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});await command('Runtime.enable');await command('Page.enable');
  await command('Page.addScriptToEvaluateOnNewDocument',{source:"sessionStorage.setItem('token','synthetic-g8-session');"});
  await command('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
  await visit();
  await check('Canonical classification and both audiences rendered','document.body.innerText.includes("THCS")&&document.body.innerText.includes("Người lớn")');
  await check('Legacy type is never displayed as canonical program','!document.body.innerText.includes("LEGACY_NOT_A_PROGRAM")&&document.body.innerText.includes("Chưa xác nhận chương trình")');
  await check('Multi-audience course occupies one table row','document.querySelectorAll("tr[data-row-key=\\"1\\"]").length===1');
  await click('[aria-label="Đối tượng học"]');
  await wait('!!document.querySelector(\'.ant-select-item[title="THCS"]\')');
  await evaluate('document.querySelector(\'.ant-select-item[title="THCS"]\').click()');
  await wait('location.search.includes("audienceId=3")&&document.querySelectorAll("tr[data-row-key]").length===1');checks.push('Audience control submits canonical ID and removes unclassified course');
  await visit();
  await evaluate('Array.from(document.querySelectorAll("button")).find(b=>b.textContent.includes("IELTS Foundation")).click()');
  await wait('!!document.querySelector(".ant-drawer-body")');
  await check('Course drawer names program explicitly without legacy fallback','document.querySelector(".ant-drawer-body").innerText.includes("Chương trình")&&!document.querySelector(".ant-drawer-body").innerText.includes("LEGACY_NOT_A_PROGRAM")');
  await visit();
  await evaluate('Array.from(document.querySelectorAll("button")).find(b=>b.textContent.includes("Thêm khóa")).click()');
  await wait('!!document.querySelector(".ant-modal-body")');
  await wait('!!document.querySelector(".ant-modal-body [id$=programId]")&&!!document.querySelector(".ant-modal-body [id$=audienceIds]")');
  await check('Creation uses program and audience fields, no free text type','!!document.querySelector(".ant-modal-body [id$=programId]")&&!!document.querySelector(".ant-modal-body [id$=audienceIds]")&&!document.querySelector(".ant-modal-body [id$=_type]")');
  await check('Audience is described as advice, separate from admission level','document.querySelector(".ant-modal-body").innerText.includes("không thay thế trình độ đầu vào")');
  const image=await command('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.join(out,'course-form.png'),Buffer.from(image.data,'base64'));
  for(const nextRole of ['ADMIN','STAFF']){role=nextRole;await visit();
    await check(nextRole+' has correct course-create visibility',`Array.from(document.querySelectorAll('button')).some(b=>b.textContent.includes('Thêm khóa'))===${nextRole==='ADMIN'}`);
  }
  role='TEACHER';await command('Page.navigate',{url:base+'/courses'});
  await wait('document.body?.innerText.includes("Bạn không có quyền truy cập")');
  checks.push('Teacher cannot open the course-management page');
  assert.deepEqual(exceptions,[]);checks.push('No uncaught browser exceptions');
  return {status:'passed',checks,requests:requests.length,upstream:'synthetic HTTP fixture',liveRecordsModified:false};
}
run().then(result=>{fs.writeFileSync(path.join(out,'result.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result,null,2));})
 .catch(async e=>{try{const png=await command('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.join(out,'failure.png'),Buffer.from(png.data,'base64'));}catch{}fs.writeFileSync(path.join(out,'result.json'),JSON.stringify({status:'failed',error:e.message,checks,requests},null,2));console.error(e.stack);process.exitCode=1;})
 .finally(()=>{ws?.close();for(const c of children)if(c.exitCode===null)try{execFileSync('taskkill.exe',['/PID',String(c.pid),'/T','/F'],{windowsHide:true,stdio:'ignore'});}catch{}server?.close();});
