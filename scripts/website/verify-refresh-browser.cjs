const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {spawn,execFileSync}=require('node:child_process');
const root=path.resolve(__dirname,'../../../../language-center-website');
const delay=ms=>new Promise(r=>setTimeout(r,ms));
module.exports=async function({bases,folder,websiteKey,extended=false}){
 const output=path.join(folder,'browser');fs.mkdirSync(output,{recursive:true});const checks=[],errors=[],children=[],pending=new Map();let ws,seq=0;
 const offset=Number(process.env.WEBSITE_VERIFY_PORT_OFFSET||0),webPort=31200+offset,debugPort=31227+offset,base='http://127.0.0.1:'+webPort;
 const launch=(exe,args,env=process.env,cwd=process.cwd())=>{const fd=fs.openSync(path.join(output,'process-'+children.length+'.log'),'w');const c=spawn(exe,args,{env,cwd,windowsHide:true,stdio:['ignore',fd,fd]});fs.closeSync(fd);children.push(c);return c;};
 function send(method,params={}){return new Promise((resolve,reject)=>{const id=++seq;const timeout=setTimeout(()=>{pending.delete(id);reject(Error(method+' timeout'));},20000);pending.set(id,{resolve,reject,timeout});ws.send(JSON.stringify({id,method,params}));});}
 async function evaluate(expression){const r=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value;}
 async function wait(expression){for(let i=0;i<150;i++){if(await evaluate(expression))return;await delay(100);}throw Error('Timeout '+expression);}
 async function visit(route){await send('Page.navigate',{url:base+route});const target=new URL(base+route);await wait('location.pathname==='+JSON.stringify(target.pathname)+'&&location.search==='+JSON.stringify(target.search)+'&&document.readyState==="complete"&&!!document.querySelector("h1")');await delay(150);}
 async function check(name,expression){assert.ok(await evaluate(expression),name);checks.push(name);}
 async function click(selector){
  await evaluate(`document.querySelector(${JSON.stringify(selector)}).scrollIntoView({block:'center',behavior:'instant'})`);
  let point,previous;
  for(let attempt=0;attempt<30;attempt++){
   point=await evaluate(`(()=>{const e=document.querySelector(${JSON.stringify(selector)}),r=e.getBoundingClientRect(),x=r.x+r.width/2,y=r.y+r.height/2,t=document.elementFromPoint(x,y);return {x,y,hit:t===e||e.contains(t)};})()`);
   if(point.hit&&previous&&Math.abs(point.x-previous.x)<.1&&Math.abs(point.y-previous.y)<.1)break;
   previous=point;await delay(100);
  }
  assert.ok(point?.hit,'Clickable target '+selector);
  const {x,y}=point;await send('Input.dispatchMouseEvent',{type:'mouseMoved',x,y});await send('Input.dispatchMouseEvent',{type:'mousePressed',x,y,button:'left',clickCount:1});await send('Input.dispatchMouseEvent',{type:'mouseReleased',x,y,button:'left',clickCount:1});
 }
 async function screenshot(name){const r=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.join(output,name+'.png'),Buffer.from(r.data,'base64'));}
 try{
  launch(process.execPath,[path.join(root,'node_modules/next/dist/bin/next'),'start','--hostname','127.0.0.1','--port',String(webPort)],{...process.env,WEBSITE_API_URL:bases.academic+'/api/v1/academic/public/catalog/',WEBSITE_FORM_SECRET:websiteKey},root);
  let ready=false;for(let i=0;i<150;i++){try{if((await fetch(base)).ok){ready=true;break;}}catch{}await delay(150);}assert.ok(ready,'Website startup');
  launch(process.env.UI_BROWSER==='edge'?'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe':'C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--remote-debugging-port='+debugPort,'--user-data-dir='+path.join(output,'browser-profile'),'about:blank']);
  let target;for(let i=0;i<100;i++){try{target=(await(await fetch('http://127.0.0.1:'+debugPort+'/json/list')).json()).find(t=>t.type==='page');if(target)break;}catch{}await delay(100);}assert.ok(target,'Browser startup');
  ws=new WebSocket(target.webSocketDebuggerUrl);ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.text);if(m.id&&pending.has(m.id)){const p=pending.get(m.id);pending.delete(m.id);clearTimeout(p.timeout);m.error?p.reject(Error(m.error.message)):p.resolve(m.result);}};
  await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});await send('Runtime.enable');await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
  await visit('/');
  await wait('document.querySelector(".catalogHeader")?.getBoundingClientRect().height>100');
  await check('Desktop navigation shows fixed audience entry points','Array.from(document.querySelectorAll(".desktopNav > a")).some(a=>a.textContent.includes("Mẫu giáo"))&&Array.from(document.querySelectorAll(".desktopNav > a")).some(a=>a.textContent.includes("Tiểu học"))&&Array.from(document.querySelectorAll(".desktopNav > a")).some(a=>a.textContent.includes("THCS"))');
  await check('IELTS and TOEIC dropdowns come from published programs','["IELTS","TOEIC"].every(label=>Array.from(document.querySelectorAll(".desktopNav summary")).some(s=>s.textContent.includes(label)))');
  await screenshot('home-desktop');
  await click('.desktopNav a[href="/khoa-hoc?audience=SECONDARY"]');await wait('location.search.includes("audience=SECONDARY")&&document.querySelectorAll(".courseCard").length===3');
  checks.push('THCS menu queries real backend and shows three distinct courses');
  await check('Active audience filter remains visible','document.querySelector("select[name=audience]").value==="SECONDARY"');
  await screenshot('thcs-desktop');
  const ielts=await evaluate('Array.from(document.querySelectorAll(".desktopNav details")).findIndex(d=>d.querySelector("summary").textContent.includes("IELTS"))');
  await click('.desktopNav details:nth-of-type('+(ielts+1)+') summary');
  await wait('Array.from(document.querySelectorAll(".desktopNav details[open]")).some(d=>d.innerText.includes("THPT"))');checks.push('IELTS dropdown offers THPT without removing the canonical group');
  await send('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await send('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
  await check('Escape closes dropdown and restores summary focus','document.querySelectorAll(".desktopNav details[open]").length===0&&document.activeElement.tagName==="SUMMARY"');
  await visit('/khoa-hoc?audience=ADULT');await check('Large group exposes pagination','!!document.querySelector(".pagination a")');await click('.pagination a');await wait('location.search.includes("page=1")&&document.querySelectorAll(".courseCard").length===4');await check('Pagination retains adult filter','location.search.includes("audience=ADULT")&&document.querySelector("select[name=audience]").value==="ADULT"');
  await visit('/khoa-hoc?audience=SECONDARY&program=ielts');await check('Combined program and audience filter has two courses','document.querySelectorAll(".courseCard").length===2&&document.querySelector("select[name=program]").value==="ielts"');
  await visit('/khoa-hoc?audience=SECONDARY&program=ielts&branch=cn01&maxTuition=3000000');await check('Empty result keeps program and branch in consultation link','document.querySelectorAll(".courseCard").length===0&&document.querySelector(".emptyCatalog a").href.includes("chuong-trinh=ielts")&&document.querySelector(".emptyCatalog a").href.includes("chi-nhanh=cn01")');
  await visit('/khoa-hoc?audience=PRESCHOOL&branch=cn01');await click('.courseCard h2 a[href^="/khoa-hoc/mg2"]');await wait('location.pathname==="/khoa-hoc/mg2"&&!!document.querySelector(".classCard")');
  await check('Selected branch survives into scoped recruiting classes','location.search.includes("branch=cn01")&&document.querySelectorAll(".classCard").length===1');
  await check('Course detail preserves selected audience context','location.search.includes("audience=PRESCHOOL")&&Array.from(document.querySelectorAll(".classCard a")).every(a=>a.href.includes("doi-tuong=PRESCHOOL"))');
  await click('.classCard a[href*="lop=cn01-l10"]');await wait('location.pathname==="/tu-van"&&document.querySelector("select[name=schoolClass]")?.value==="cn01-l10"&&!document.querySelector(".submitButton").disabled');
  await check('Consultation preselects real program, course, branch and class','document.querySelector("select[name=program]").value==="tieng-anh-tong-quat"&&document.querySelector("select[name=course]").value==="mg2"&&document.querySelector("select[name=branch]").value==="cn01"');
  await check('Editable consultation note retains preschool interest','document.querySelector("textarea[name=note]").value.includes("Mẫu giáo")');
  await evaluate('(()=>{for(const [name,value] of [["fullName","Nguyễn An"],["phone","0987000001"]]){const e=document.querySelector(`input[name=${name}]`);Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,"value").set.call(e,value);e.dispatchEvent(new Event("input",{bubbles:true}));}document.querySelector("input[name=consent]").click();})()');
  await click('.submitButton');await wait('!!document.querySelector(".formSuccess")');checks.push('Actual BFF submits one consultation to the real academic API on the private database');await screenshot('consultation-success');
  for(const width of [390,320]){await send('Emulation.setDeviceMetricsOverride',{width,height:900,deviceScaleFactor:1,mobile:true});await visit('/khoa-hoc?audience=PRIMARY');await click('.menuButton');await wait('!!document.querySelector("#mobile-navigation")');await check('Mobile '+width+'px menu has no horizontal page overflow','document.documentElement.scrollWidth<=innerWidth');await click('#mobile-navigation summary');await check('Mobile dropdown expands within screen','document.querySelector("#mobile-navigation .navDropdown[open]")!==null&&document.documentElement.scrollWidth<=innerWidth');await screenshot('mobile-menu-'+width);await send('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await send('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});}
  let extendedResult;if(extended)extendedResult=await require('./verify-ui-u7.cjs')({evaluate,visit,check,screenshot,send,wait});
  if(extendedResult)fs.writeFileSync(path.join(output,'u7-ui-details.json'),JSON.stringify(extendedResult,null,2));
  assert.deepEqual(errors,[]);checks.push('No uncaught browser exceptions');
  const staticDir=path.join(root,process.env.WEBSITE_BUILD_DIR||'.next','static');function scan(dir){for(const entry of fs.readdirSync(dir,{withFileTypes:true})){const file=path.join(dir,entry.name);if(entry.isDirectory())scan(file);else if(file.endsWith('.js'))assert.ok(!fs.readFileSync(file,'utf8').includes(websiteKey),'Private BFF key leaked');}}scan(staticDir);checks.push('Private BFF key is absent from browser bundles');
  const result={status:'passed',checks,upstream:'real API Gateway and academic/auth/teaching/finance services on isolated MySQL copy',screenshots:path.relative(process.cwd(),output)};fs.writeFileSync(path.join(output,'result.json'),JSON.stringify(result,null,2));return result;
 }catch(e){try{await screenshot('failure');fs.writeFileSync(path.join(output,'failure-body.txt'),await evaluate('document.body?.innerText||""'));}catch{}fs.writeFileSync(path.join(output,'result.json'),JSON.stringify({status:'failed',message:e.message,checks,errors},null,2));throw e;}
 finally{ws?.close();for(const c of children)if(c.exitCode===null&&c.pid)try{execFileSync('taskkill.exe',['/PID',String(c.pid),'/T','/F'],{stdio:'ignore',windowsHide:true});}catch{}}
};
