import fs from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { randomUUID } from 'node:crypto';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const web=path.resolve(root,'../../language-center-web-frontend');
const output=path.join(root,'storage/c6-ui');fs.mkdirSync(output,{recursive:true});
const report={data:'Synthetic API fixtures + real built web/Chromium; no Gemini or database',checks:[],errors:[]};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function port(){const server=net.createServer();await new Promise(r=>server.listen(0,'127.0.0.1',r));const p=server.address().port;await new Promise(r=>server.close(r));return p;}
const webPort=await port(), chromePort=await port(), base='http://127.0.0.1:'+webPort;
const children=[];let socket;const pending=new Map();let next=0;
let role='ADMIN',enabled=true,configStatus=200,held=[],retryCount=0,loadedScript;
const requests=[];
const scopeFor=request=>({mode:request.scopeMode??'ACTIVE_BRANCH',branchIds:request.scopeMode==='WHOLE_SYSTEM'?[]:request.scopeMode==='ALL_ASSIGNED'?[1,2]:[Number(request._branch??1)],label:request.scopeMode==='WHOLE_SYSTEM'?'Toàn hệ thống':request.scopeMode==='ALL_ASSIGNED'?'Các chi nhánh được phân công':'Chi nhánh '+(request._branch??1)});
function reply(body,patch={}){return {requestId:body.requestId,conversationId:body.conversationId??randomUUID(),status:'ANSWER',message:'Đang hiển thị 1 trên 1 kết quả trong phạm vi đang xem.',scope:scopeFor(body),observedAt:new Date().toISOString(),cards:[{tool:'search_courses',title:'Khóa học',data:{content:[{id:1,code:'GT1',name:'Giao tiếp',tuitionFee:4500000,branchId:1,path:'/courses?keyword=GT1&branchId=1'}],totalElements:1}}],choices:[],sources:[{tool:'search_courses',observedAt:new Date().toISOString(),scope:scopeFor(body)}],warnings:[],hasMore:false,nextPage:null,...patch};}
function cdp(method,params={}){return new Promise((resolve,reject)=>{const id=++next;const timer=setTimeout(()=>{pending.delete(id);reject(Error('CDP timeout '+method));},15000);pending.set(id,{resolve,reject,timer});socket.send(JSON.stringify({id,method,params}));});}
async function ev(expression){const r=await cdp('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description??r.exceptionDetails.text);return r.result.value;}
async function wait(expression,label=expression){const end=Date.now()+15000;while(Date.now()<end){if(await ev('Boolean('+expression+')'))return;await sleep(100);}throw Error('UI timeout '+label+' '+String(await ev('document.body.innerText')).slice(-600));}
const text=()=>ev('document.body.innerText');
const button=(label)=>`[...document.querySelectorAll('button')].find(e=>e.getBoundingClientRect().width>0&&e.textContent.trim()===${JSON.stringify(label)})`;
async function click(expression){await sleep(160);const point=await ev(`(()=>{const e=${expression};if(!e)throw Error('Missing control');e.scrollIntoView({block:'center'});const r=e.getBoundingClientRect();return{x:r.x+r.width/2,y:r.y+r.height/2}})()`);await cdp('Input.dispatchMouseEvent',{type:'mousePressed',...point,button:'left',clickCount:1});await cdp('Input.dispatchMouseEvent',{type:'mouseReleased',...point,button:'left',clickCount:1});}
async function fill(value){await ev(`(()=>{const e=document.querySelector('textarea[aria-label="Câu hỏi cho trợ lý"]');e.focus();e.select()})()`);await cdp('Input.insertText',{text:value});}
async function ask(value){await fill(value);await click(button('Gửi'));await wait(`!document.querySelector('[role="log"][aria-busy="true"]')`,'chat completed');}
async function check(name,run){await run();report.checks.push({name,status:'PASS'});fs.writeFileSync(path.join(output,'result.json'),JSON.stringify(report,null,2));console.log('PASS '+name);}
async function screenshot(name){const r=await cdp('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.join(output,name+'.png'),Buffer.from(r.data,'base64'));}
async function fulfill(event,status=200,body={},headers=[]){try{await cdp('Fetch.fulfillRequest',{requestId:event.requestId,responseCode:status,responseHeaders:[{name:'Content-Type',value:'application/json'},{name:'Access-Control-Allow-Origin',value:'*'},{name:'Access-Control-Allow-Methods',value:'GET,POST,DELETE,OPTIONS'},{name:'Access-Control-Allow-Headers',value:'Authorization,Content-Type,X-Branch-Id,X-Request-Id'},{name:'Access-Control-Expose-Headers',value:'Retry-After'},...headers],body:Buffer.from(status===204?'':JSON.stringify(body)).toString('base64')});}catch(error){if(!/Invalid InterceptionId|Invalid interceptionId|No resource|Invalid RequestId/.test(error.message))throw error;}}
async function intercept(event){
  const url=new URL(event.request.url), pathname=url.pathname;
  if(!/\/(?:auth|academic|finance|teaching|assistant)\//.test(pathname)){
    if(url.origin===base||url.protocol==='data:')return cdp('Fetch.continueRequest',{requestId:event.requestId});
    return cdp('Fetch.failRequest',{requestId:event.requestId,errorReason:'BlockedByClient'});
  }
  if(event.request.method==='OPTIONS')return fulfill(event,204);
  const p=pathname.replace(/^.*\/api\/v1/,'');
  const branch=Object.entries(event.request.headers).find(([k])=>k.toLowerCase()==='x-branch-id')?.[1];
  let body={};try{body=JSON.parse(event.request.postData??'{}');}catch{}
  requests.push({path:p,method:event.request.method,role,branch,body});
  if(p==='/auth/me')return fulfill(event,200,{accountId:100,profileId:11,username:'operator',fullName:'Người dùng '+role,role,branchId:role==='SUPER_ADMIN'?null:1,branchIds:role==='SUPER_ADMIN'?[]:role==='ADMIN'?[1,2]:[1],status:true,mustChangePassword:false,email:null,phone:null});
  if(p==='/academic/branches')return fulfill(event,200,{content:[{id:1,code:'CN1',name:'Chi nhánh 1',status:'ACTIVE'},{id:2,code:'CN2',name:'Chi nhánh 2',status:'ACTIVE'}],page:0,totalPages:1,totalElements:2,size:200});
  if(p==='/academic/branches/1')return fulfill(event,200,{id:1,code:'CN1',name:'Chi nhánh 1',status:'ACTIVE'});
  if(p==='/assistant/config')return fulfill(event,configStatus,{enabled,readOnly:true,role,maxMessageLength:2000,conversationTtlMinutes:15});
  if(p.startsWith('/assistant/conversations/'))return fulfill(event,204);
  if(p==='/assistant/chat'){
    body._branch=branch;const q=body.message.toLowerCase();
    if(q.includes('chậm')){held.push({event,body});return;}
    if(q.includes('429'))return fulfill(event,429,{code:'MODEL_RATE_LIMITED' },[{name:'Retry-After',value:'2'}]);
    if(q.includes('409'))return fulfill(event,409,{code:'CONTEXT_CHANGED'});
    if(q.includes('403'))return fulfill(event,403,{code:'FORBIDDEN'});
    if(q.includes('503'))return fulfill(event,503,{code:'MODEL_UNAVAILABLE'});
    if(q.includes('thử lại')&&retryCount++===0)return fulfill(event,503,{code:'MODEL_UNAVAILABLE'});
    if(q.includes('trùng'))return fulfill(event,200,reply(body,{status:'CLARIFICATION_REQUIRED',message:'Có nhiều học viên phù hợp. Chọn đúng người.',cards:[],choices:[{type:'STUDENT',id:11,code:'HV11',label:'Nguyễn Văn An',branchId:1},{type:'STUDENT',id:12,code:'HV12',label:'Nguyễn Văn An',branchId:1}]}));
    if(body.selection)return fulfill(event,200,reply(body,{cards:[{tool:'get_student_classes',title:'Lớp học',data:{content:[{id:1,classCode:'GT1',className:'Giao tiếp 1',studyStatus:'STUDYING',branchId:1,path:role==='TEACHER'?'/my-classes?classId=1':'/classes?classId=1'}]}}]}));
    if(q.includes('tổng'))return fulfill(event,200,reply(body,{message:'Tổng số hồ sơ học viên trong phạm vi đang xem: 0.',cards:[{tool:'get_student_statistics',title:'Thống kê học viên',data:{metric:'TOTAL_PROFILES',value:0}}]}));
    if(q.includes('điểm'))return fulfill(event,200,reply(body,{cards:[{tool:'get_learning_results',title:'Kết quả học tập',data:{records:{content:[{studentName:'An',score:0},{studentName:'Bình',score:null}]},view:'CURRENT',maxScore:10}}]}));
    if(q.includes('rỗng'))return fulfill(event,200,reply(body,{status:'NO_RESULTS',message:'Không tìm thấy dữ liệu phù hợp trong phạm vi đang xem.',cards:[]}));
    if(q.includes('tiêm'))return fulfill(event,200,reply(body,{cards:[{tool:'search_courses',title:'Khóa học',data:{content:[{name:'<img src=x onerror=window.__xss=1>',path:'https://evil.invalid',tuitionFee:0}]}}]}));
    if(q.includes('liên chi'))return fulfill(event,200,reply(body,{cards:[{tool:'search_courses',title:'Khóa học',data:{content:[{code:'GT2',name:'Giao tiếp 2',branchId:2,path:'/courses?keyword=GT2&branchId=2'}]}}]}));
    if(q.includes('trang'))return fulfill(event,200,reply(body,{hasMore:true,nextPage:1}));
    return fulfill(event,200,reply(body));
  }
  return fulfill(event,200,{content:[],page:0,size:20,totalElements:0,totalPages:0});
}
async function actor(nextRole){role=nextRole;
  const token='fixture.'+Buffer.from(JSON.stringify({accountId:100,role,exp:Math.floor(Date.now()/1000)+3600})).toString('base64url')+'.synthetic';
  if(loadedScript)await cdp('Page.removeScriptToEvaluateOnNewDocument',{identifier:loadedScript});
  loadedScript=(await cdp('Page.addScriptToEvaluateOnNewDocument',{source:`sessionStorage.clear();localStorage.clear();sessionStorage.setItem('token',${JSON.stringify(token)});` })).identifier;
  await cdp('Page.navigate',{url:base+'/profile'});await wait(`document.body.innerText.includes(${JSON.stringify('Người dùng '+role)})`,'actor '+role);
  await click(button('Trợ lý'));await wait(`document.querySelector('textarea[aria-label="Câu hỏi cho trợ lý"]') || document.body.innerText.includes('Kiểm tra lại kết nối')`,'assistant open');
}
try{
  const vite=spawn(process.execPath,[path.join(web,'node_modules/vite/bin/vite.js'),'preview','--host','127.0.0.1','--port',String(webPort),'--strictPort'],{cwd:web,windowsHide:true,stdio:'ignore'});children.push(vite);
  for(let i=0;i<100;i++){try{if((await fetch(base)).ok)break;}catch{}await sleep(100);}
  const chrome=spawn('C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--no-first-run','--no-default-browser-check','--remote-debugging-port='+chromePort,'--user-data-dir='+path.join(output,'profile-'+Date.now()),'about:blank'],{windowsHide:true,stdio:'ignore'});children.push(chrome);
  let targets;for(let i=0;i<100;i++){try{targets=await(await fetch('http://127.0.0.1:'+chromePort+'/json/list')).json();if(targets.some(t=>t.type==='page'))break;}catch{}await sleep(100);}
  const target=targets.find(t=>t.type==='page');socket=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j;});
  socket.onmessage=({data})=>{const m=JSON.parse(data);if(m.id){const p=pending.get(m.id);if(p){clearTimeout(p.timer);pending.delete(m.id);m.error?p.reject(Error(m.error.message)):p.resolve(m.result);}return;}
    if(m.method==='Fetch.requestPaused')void intercept(m.params).catch(error=>report.errors.push(error.message));
    if(m.method==='Runtime.exceptionThrown')report.errors.push(m.params.exceptionDetails.exception?.description??m.params.exceptionDetails.text);
  };
  await cdp('Page.enable');await cdp('Runtime.enable');await cdp('Fetch.enable',{patterns:[{urlPattern:'*'}]});
  await cdp('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});
  await check('Four roles: launcher, scope and starter permissions',async()=>{for(const r of ['SUPER_ADMIN','ADMIN','STAFF','TEACHER']){await actor(r);assert.match(await text(),/Tra cứu nhanh/);if(r==='TEACHER'){assert.doesNotMatch(await ev('document.querySelector("[role=log]").innerText'),/Tổng số|khách hàng/);}else if(r==='SUPER_ADMIN')assert.match(await text(),/Toàn hệ thống/);}});
  await actor('ADMIN');
  await check('Money card and keyboard Enter submit',async()=>{await fill('Khóa giao tiếp dưới 5 triệu');await cdp('Input.dispatchKeyEvent',{type:'keyDown',key:'Enter',code:'Enter',windowsVirtualKeyCode:13});await cdp('Input.dispatchKeyEvent',{type:'keyUp',key:'Enter',code:'Enter',windowsVirtualKeyCode:13});await wait('document.querySelector("[role=log]").innerText.includes("4.500.000")');assert.match(await text(),/Mở chi tiết/);});
  await screenshot('desktop');
  await check('Duplicate student choices continue same conversation with selected ID',async()=>{await ask('Tìm học viên trùng tên');await click(`[...document.querySelectorAll('button')].find(e=>e.textContent.includes('HV12')&&e.textContent.includes('Nguyễn'))`);await wait('document.querySelector("[role=log]").innerText.includes("Giao tiếp 1")');const calls=requests.filter(r=>r.path==='/assistant/chat');assert.equal(calls.at(-1).body.selection.id,12);assert.ok(calls.at(-1).body.conversationId);});
  await check('Pagination keeps server conversation',async()=>{await ask('Xem trang khóa học');const prev=requests.filter(r=>r.path==='/assistant/chat').at(-1).body.conversationId;await click(button('Xem thêm kết quả'));await wait('!document.querySelector("[role=log][aria-busy=true]")');assert.equal(requests.filter(r=>r.path==='/assistant/chat').at(-1).body.conversationId,prev);});
  await check('0 statistics, 0 grade and ungraded null stay distinct',async()=>{await ask('Tổng học viên');assert.match(await text(),/: 0\./);await ask('Điểm lớp');assert.match(await text(),/Chưa chấm/);assert.match(await ev('document.querySelector("[role=log]").innerText'),/Thang điểm/);});
  await check('Empty and forbidden are separate states',async()=>{await ask('Kết quả rỗng');assert.match(await text(),/Không tìm thấy dữ liệu/);await ask('403');assert.match(await text(),/không có quyền/);});
  await check('Retry uses same idempotency UUID after temporary failure',async()=>{retryCount=0;await ask('Thử lại khóa học');const first=requests.filter(r=>r.path==='/assistant/chat').at(-1).body.requestId;await click(button('Thử lại yêu cầu'));await wait('!document.querySelector("[role=log][aria-busy=true]")');assert.equal(requests.filter(r=>r.path==='/assistant/chat').at(-1).body.requestId,first);});
  await check('429 shows Retry-After countdown and blocks sending',async()=>{await ask('429');assert.match(await text(),/Có thể gửi lại sau/);assert.equal(await ev(`${button('Gửi')}?.disabled`),true);await wait('!document.body.innerText.includes("Có thể gửi lại sau")');});
  await check('HTML and external links render as safe text',async()=>{await ask('Tiêm dữ liệu');assert.equal(await ev('window.__xss??0'),0);assert.equal(await ev('document.querySelector("[role=log] img")'),null);assert.match(await text(),/<img src=x/);assert.equal(await ev('document.querySelector("[role=log] a[href*=evil]")'),null);});
  await check('History is not copied into browser storage',async()=>{const stored=await ev('JSON.stringify({...localStorage,...sessionStorage})');assert.doesNotMatch(stored,/Trợ lý|Tiêm|conversationId|requestId/);});
  await check('409 clears prior results and requires a new conversation',async()=>{await ask('409');assert.match(await text(),/phạm vi đã thay đổi/);assert.doesNotMatch(await ev('document.querySelector("[role=log]").innerText'),/img src=x|4.500.000/);});
  await check('Scope change clears history and switches request mode',async()=>{await click(`document.querySelector('[aria-label="Phạm vi tra cứu"]')?.closest('.ant-select')`);await click(`[...document.querySelectorAll('.ant-select-item-option')].find(e=>e.textContent.includes('Các chi nhánh'))`);await ask('Tổng học viên');assert.equal(requests.filter(r=>r.path==='/assistant/chat').at(-1).body.scopeMode,'ALL_ASSIGNED');assert.equal(requests.filter(r=>r.path==='/assistant/chat').at(-1).body.conversationId,undefined);});
  await check('Admin cross-branch result confirms before navigating',async()=>{await ask('Khóa học liên chi nhánh');await click(`[...document.querySelectorAll('[role=log] button')].filter(e=>e.textContent.includes('Mở chi tiết')).at(-1)`);await wait('document.body.innerText.includes("Chuyển chi nhánh để mở kết quả?")');await click(button('Chuyển và mở'));await wait('location.pathname==="/courses"');assert.equal(await ev('new URL(location.href).searchParams.get("branchId")'),'2');assert.equal(await ev('sessionStorage.getItem("lc.branch.100")'),'2');});
  await actor('ADMIN');
  await check('Closing aborts pending wait; SPA account switch discards late result',async()=>{await fill('Kết quả chậm');await click(button('Gửi'));while(!held.length)await sleep(50);await click(`document.querySelector('.ant-drawer-close')`);await sleep(200);
    role='TEACHER';const replacement='fixture.'+Buffer.from(JSON.stringify({accountId:100,role,exp:Math.floor(Date.now()/1000)+3600})).toString('base64url')+'.synthetic';
    await ev(`sessionStorage.setItem('token',${JSON.stringify(replacement)});window.dispatchEvent(new Event('language-center:session'))`);
    await wait('document.body.innerText.includes("Người dùng TEACHER")');await click(button('Trợ lý'));await wait('document.querySelector("[role=log]")');
    for(const h of held.splice(0))await fulfill(h.event,200,reply(h.body,{message:'OLD_PRIVATE_RESULT'}));await sleep(300);assert.doesNotMatch(await text(),/OLD_PRIVATE_RESULT|Kết quả chậm/);});
  await check('Teacher course result offers no forbidden management link',async()=>{await ask('Khóa giao tiếp');assert.equal(await ev('[...document.querySelectorAll("[role=log] button")].some(e=>e.textContent.includes("Mở chi tiết"))'),false);});
  await check('Mobile drawer fits viewport and remains usable',async()=>{await cdp('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});await actor('STAFF');await ask('Khóa giao tiếp');assert.equal(await ev('document.documentElement.scrollWidth<=innerWidth'),true);const width=await ev('document.querySelector(".ant-drawer-content-wrapper").getBoundingClientRect().width');assert.ok(width<=391);await screenshot('mobile');});
  await check('New conversation deletes only the current owned conversation',async()=>{await click(button('Hội thoại mới'));await wait('document.body.innerText.includes("Nội dung hội thoại hiện tại sẽ được xóa")');await click(`[...document.querySelectorAll('.ant-popconfirm button')].find(e=>e.textContent==='Hội thoại mới')`);await wait('!document.querySelector("[role=log][aria-busy=true]")');assert.match(await ev('document.querySelector("[role=log]").innerText'),/Bạn cần tra cứu gì/);assert.equal(requests.at(-1).method,'DELETE');});
  await check('Escape closes drawer and restores launcher focus',async()=>{await sleep(450);await fill('');await cdp('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await cdp('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await wait('!document.querySelector(".ant-drawer-open")');await wait('document.activeElement.textContent.trim()==="Trợ lý"','launcher focus restored');});
  await check('404 config shows connection error and retry recovers without expired conversation message',async()=>{configStatus=404;await actor('ADMIN');await wait('document.body.innerText.includes("Chưa kết nối được dịch vụ trợ lý")');assert.doesNotMatch(await text(),/Hội thoại đã hết hạn/);configStatus=200;await click(button('Kiểm tra lại kết nối'));await wait(`document.querySelector('textarea[aria-label="Câu hỏi cho trợ lý"]')`);});
  await check('Disabled config has a useful state without developer/provider labels',async()=>{enabled=false;await actor('ADMIN');assert.match(await text(),/chưa sẵn sàng/);assert.doesNotMatch(await text(),/GEMINI|MCP|sandbox|API key/);assert.equal(await ev(`document.querySelector('textarea[aria-label="Câu hỏi cho trợ lý"]')`),null);});
  await check('Logout unmounts assistant and hides all conversation state',async()=>{await ev(`sessionStorage.removeItem('token');window.dispatchEvent(new Event('language-center:session'))`);await wait('location.pathname==="/login"');assert.equal(await ev(`${button('Trợ lý')}??null`),null);assert.equal(await ev('document.querySelector("[role=log]")'),null);});
  assert.deepEqual(report.errors,[]);report.status='PASS';
}catch(error){report.status='FAIL';report.error=error.stack;console.error(error);process.exitCode=1;
}finally{
  fs.writeFileSync(path.join(output,'result.json'),JSON.stringify(report,null,2));
  if(socket?.readyState===WebSocket.OPEN)await Promise.race([cdp('Browser.close').catch(()=>{}),sleep(1500)]);
  for(const item of pending.values())clearTimeout(item.timer);socket?.close();
  for(const child of children.reverse()){try{child.kill();}catch{}}
}
