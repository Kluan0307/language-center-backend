const fs=require('node:fs');
const report={date:'2026-11-11',mode:'Web rendering at mobile viewport; not Android native device',checks:[],errors:[]};
let ws,id=0;const pending=new Map();
function send(method,params={}){return new Promise((resolve,reject)=>{const key=++id,timer=setTimeout(()=>{pending.delete(key);reject(Error('CDP timeout '+method))},20000);pending.set(key,{resolve,reject,timer});ws.send(JSON.stringify({id:key,method,params}));});}
async function ev(expression){const r=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value;}
async function wait(expression){for(let n=0;n<100;n++){if(await ev(expression))return;await new Promise(r=>setTimeout(r,300));}throw Error('Timeout: '+expression);}
async function click(label){await ev(`(()=>{const e=[...document.querySelectorAll('[role="button"],a')].find(e=>e.innerText?.includes(${JSON.stringify(label)})||e.getAttribute('aria-label')===${JSON.stringify(label)});if(!e)throw Error('Missing button');e.click()})()`);}
async function check(name,fn){await fn();report.checks.push({name,status:'PASS'});}
(async()=>{try{
let tabs; for(let attempt=0;attempt<40;attempt++){try{tabs=await(await fetch('http://127.0.0.1:19389/json/list')).json();if(tabs.some(t=>t.type==='page'))break;}catch{}await new Promise(r=>setTimeout(r,250));}if(!tabs)throw Error('Browser not ready');const tab=tabs.find(t=>t.type==='page');ws=new WebSocket(tab.webSocketDebuggerUrl);await new Promise(r=>ws.addEventListener('open',r));ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);if(p){clearTimeout(p.timer);pending.delete(m.id);m.error?p.reject(Error(m.error.message)):p.resolve(m.result)}}if(m.method==='Runtime.exceptionThrown')report.errors.push(m.params.exceptionDetails.exception?.description||m.params.exceptionDetails.text)};
await send('Runtime.enable');await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});await send('Page.navigate',{url:'http://localhost:18090'});
await check('Welcome renders without session',()=>wait('document.body.innerText.includes("Chào mừng đến Language Center")'));
await check('Development preview opens',async()=>{await click('Khám phá giao diện');await wait('document.body.innerText.includes("Truy cập nhanh")')});
await check('Five tabs present',async()=>{const count=await ev('document.querySelectorAll("[role=tab]").length');if(count!==5)throw Error('Expected5 tabs got '+count)});
await check('Schedule opens from home',async()=>{await click('Lịch học của tôi');await wait('document.body.innerText.includes("Theo dõi lịch cá nhân")')});
await check('Learning tab',async()=>{await ev('document.querySelectorAll("[role=tab]")[2].click()');await wait('document.body.innerText.includes("Lớp học, kết quả")')});
await check('Tuition tab',async()=>{await ev('document.querySelectorAll("[role=tab]")[3].click()');await wait('document.body.innerText.includes("Theo dõi công nợ")')});
await check('Notifications stack',async()=>{await click('Mở thông báo');await wait('document.body.innerText.includes("Cập nhật về lịch học")')});
await send('Page.navigate',{url:'http://localhost:18090'});await wait('document.body.innerText.includes("Khám phá giao diện")');await click('Khám phá giao diện');await wait('document.querySelectorAll("[role=tab]").length===5');
await check('Profile and exit preview',async()=>{await ev('document.querySelectorAll("[role=tab]")[4].click()');await wait('document.body.innerText.includes("Thoát xem trước")');await click('Thoát xem trước');await wait('document.body.innerText.includes("Khám phá giao diện")')});
await check('Protected direct link does not bypass session',async()=>{await send('Page.navigate',{url:'http://localhost:18090/notifications'});await wait('document.body.innerText.includes("Chào mừng đến Language Center")')});
await click('Khám phá giao diện');await wait('document.body.innerText.includes("Truy cập nhanh")');const shot=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync('storage/student-phase2-home.png',Buffer.from(shot.data,'base64'));
if(report.errors.length)throw Error('Browser runtime errors');report.status='PASS';
}catch(e){report.status='FAIL';report.failure=e.message;if(ws?.readyState===1)report.dom=await ev('document.body.innerText').catch(()=>null);process.exitCode=1;}finally{fs.writeFileSync('storage/student-phase2-ui.json',JSON.stringify(report,null,2));ws?.close();console.log(JSON.stringify(report));}})();




