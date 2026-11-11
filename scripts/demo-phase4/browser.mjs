import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';
const dir=fs.readFileSync('storage/demo-phase4-latest.txt','utf8').trim();process.env.ACCEPTANCE_RUN_DIR=dir;
const {runtime,loadState,secrets,ok}=await import('../acceptance/client.mjs');const s=loadState();
const report={mode:'Real UI reads with API-authenticated sessions; no mocked requests and no UI login claim',database:runtime.database,status:'RUNNING',checks:[]};
const file=path.join(dir,'browser-demo.json');if(fs.existsSync(file))fs.copyFileSync(file,path.join(dir,'browser-demo-attempt-'+Date.now()+'.json'));
const page=await(await fetch('http://127.0.0.1:'+runtime.browserPort+'/json/new?about:blank',{method:'PUT'})).json(),ws=new WebSocket(page.webSocketDebuggerUrl);await new Promise(r=>ws.addEventListener('open',r,{once:true}));let next=0;const pending=new Map();
function cdp(method,params={}){return new Promise((resolve,reject)=>{const id=++next;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}));});}
ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);if(p)m.error?p.reject(Error(m.error.message)):p.resolve(m.result);}};
async function ev(expression){const r=await cdp('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value;}
async function wait(expression){for(let i=0;i<150;i++){if(await ev('Boolean('+expression+')'))return;await new Promise(r=>setTimeout(r,200));}throw Error('UI wait failed: '+expression);}
async function click(expression){const p=await ev('(()=>{const e='+expression+';if(!e)throw Error("Missing control");e.scrollIntoView({block:"center"});const r=e.getBoundingClientRect();return{x:r.x+r.width/2,y:r.y+r.height/2}})()');await cdp('Input.dispatchMouseEvent',{type:'mousePressed',...p,button:'left',clickCount:1});await cdp('Input.dispatchMouseEvent',{type:'mouseReleased',...p,button:'left',clickCount:1});}
let authScript;
async function actor(user){await ok('/auth/me',user);if(authScript)await cdp('Page.removeScriptToEvaluateOnNewDocument',{identifier:authScript});authScript=(await cdp('Page.addScriptToEvaluateOnNewDocument',{source:'sessionStorage.setItem("token",'+JSON.stringify(user.token)+')'})).identifier;}
async function go(route){await cdp('Page.navigate',{url:'http://127.0.0.1:'+runtime.frontendPort+route});}
async function check(id,wanted,action){const record={id,expected:wanted};report.checks.push(record);try{record.actual=await action();assert.deepEqual(record.actual,wanted);record.passed=true;}catch(e){record.passed=false;record.error=e.message;throw e;}finally{fs.writeFileSync(file,JSON.stringify(report,null,2));}}
async function picture(name){await new Promise(r=>setTimeout(r,400));fs.writeFileSync(path.join(dir,'demo-'+name+'.png'),Buffer.from((await cdp('Page.captureScreenshot',{format:'png'})).data,'base64'));}
try{
 await cdp('Page.enable');await cdp('Runtime.enable');await cdp('Emulation.setDeviceMetricsOverride',{width:1500,height:1000,deviceScaleFactor:1,mobile:false});
 await actor(secrets.accounts.sa);await go('/schedule?date='+s.nextMonday+'&calendarView=week');
 await check('UI01-future-calendar',6,async()=>{await wait('document.querySelectorAll(".fc-event").length===6');return ev('document.querySelectorAll(".fc-event").length');});await picture('calendar');
 await actor(secrets.accounts.teacher);await go('/my-schedule?date='+s.nextMonday+'&calendarView=week');
 await check('UI02-teacher-scope',[4,false],async()=>{await wait('document.querySelectorAll(".fc-event").length===4');return[await ev('document.querySelectorAll(".fc-event").length'),await ev('[...document.querySelectorAll(".fc-event")].some(e=>e.innerText.includes("TOEIC"))')];});await picture('teacher');
 await actor(secrets.accounts.admin);await go('/grades?classId='+s.schoolClass.id+'&view=final');
 await check('UI03-final-outcomes',true,async()=>{await wait('document.body.innerText.includes("Chưa đạt")&&document.body.innerText.includes("Đạt")');return true;});await picture('results');
 await actor(secrets.accounts.staff);await go('/students?studentId='+s.student.id);
 await check('UI04-learning-profile',true,async()=>{await wait('document.querySelector(".ant-drawer")?.innerText.includes("Hồ sơ học tập")');await click('[...document.querySelectorAll(".ant-drawer .ant-tabs-tab")].find(e=>e.textContent.includes("Hồ sơ học tập"))');await wait('document.querySelector(".ant-drawer")?.innerText.includes("Đạt chính thức")');return true;});await picture('student');
 report.status='PASSED';report.finishedAt=new Date().toISOString();fs.writeFileSync(file,JSON.stringify(report,null,2));console.log('PASS:4 UI reads backed by real services/MySQL.');
}catch(e){report.status='FAILED';report.error=e.message;fs.writeFileSync(file,JSON.stringify(report,null,2));await picture('failed');console.error(e.message);process.exitCode=1;}finally{ws.close();}
