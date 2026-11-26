// U1-U3 read-only acceptance against the running website. No API writes or form submissions.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {spawn,execFileSync}=require('node:child_process');
const output=path.resolve(__dirname,'../../tmp/website-ui-u123');fs.mkdirSync(output,{recursive:true});
const base=process.env.UI_TEST_URL||'http://127.0.0.1:3000',pending=new Map(),checks=[],pages=[],errors=[],consoleMessages=[];
let chrome,ws,sequence=0;
const delay=ms=>new Promise(r=>setTimeout(r,ms));
function send(method,params={}){return new Promise((resolve,reject)=>{const id=++sequence,timeout=setTimeout(()=>{pending.delete(id);reject(Error(method+' timeout'));},20000);pending.set(id,{resolve,reject,timeout});ws.send(JSON.stringify({id,method,params}));});}
async function evaluate(expression){const r=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value;}
async function wait(expression){for(let n=0;n<150;n++){if(await evaluate(expression))return;await delay(100);}throw Error('Timeout: '+expression);}
async function check(name,expression){assert.ok(await evaluate(expression),name);checks.push(name);}
async function click(selector){const point=await evaluate(`(()=>{const e=document.querySelector(${JSON.stringify(selector)});if(!e)throw Error('Missing '+${JSON.stringify(selector)});e.scrollIntoView({block:'center',behavior:'instant'});const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2};})()`);await send('Input.dispatchMouseEvent',{type:'mousePressed',...point,button:'left',clickCount:1});await send('Input.dispatchMouseEvent',{type:'mouseReleased',...point,button:'left',clickCount:1});}
async function visit(route,width=1440,height=1000){await send('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:width<700});await send('Page.navigate',{url:base+route});await wait('location.pathname==='+JSON.stringify(route.split('?')[0])+'&&document.readyState==="complete"&&!!document.querySelector("h1")');await evaluate('document.fonts.ready.then(()=>true)');await wait('document.documentElement.style.getPropertyValue("--header-height")!==""');await evaluate('scrollTo({top:0,behavior:"instant"})');}
async function fonts(selector){const doc=await send('DOM.getDocument',{depth:0});const node=await send('DOM.querySelector',{nodeId:doc.root.nodeId,selector});assert.ok(node.nodeId,'Font element '+selector);return (await send('CSS.getPlatformFontsForNode',{nodeId:node.nodeId})).fonts;}
async function screenshot(name,full=false){const png=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:full,...(full?{clip:await evaluate('({x:0,y:0,width:innerWidth,height:document.documentElement.scrollHeight,scale:1})')}:{})});fs.writeFileSync(path.join(output,name+'.png'),Buffer.from(png.data,'base64'));}
(async()=>{try{
assert.equal((await fetch(base)).status,200);
chrome=spawn('C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--remote-debugging-port=19339','--user-data-dir='+path.join(output,'chrome-profile-'+Date.now()),'about:blank'],{windowsHide:true,stdio:'ignore'});
let target;for(let n=0;n<100;n++){try{target=(await(await fetch('http://127.0.0.1:19339/json/list')).json()).find(x=>x.type==='page');if(target)break;}catch{}await delay(100);}assert.ok(target);
ws=new WebSocket(target.webSocketDebuggerUrl);ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.text);if(m.method==='Runtime.consoleAPICalled')consoleMessages.push({type:m.params.type,args:m.params.args.map(a=>a.value||a.description)});if(m.id&&pending.has(m.id)){const p=pending.get(m.id);pending.delete(m.id);clearTimeout(p.timeout);m.error?p.reject(Error(m.error.message)):p.resolve(m.result);}};await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});
for(const method of ['Page.enable','Runtime.enable','DOM.enable','CSS.enable'])await send(method);
for(const width of [320,390,600,720,768,800,1024,1100,1101,1280,1366,1440]){
 await visit('/',width,width===1366?768:1000);
 await check('Home '+width+' has no horizontal overflow','document.documentElement.scrollWidth<=innerWidth');
 await check('Home '+width+' primary action visible before image','document.querySelector("[class*=heroActions] .button").getBoundingClientRect().bottom < document.querySelector("[class*=imageFrame]").getBoundingClientRect().bottom');
 const data=await evaluate(`(()=>{const h=document.querySelector('h1'),header=document.querySelector('.catalogHeader');return {width:innerWidth,headerHeight:header.getBoundingClientRect().height,headingSize:getComputedStyle(h).fontSize,headingHeight:h.getBoundingClientRect().height,ctaTop:document.querySelector('[class*=heroActions] .button').getBoundingClientRect().top};})()`);
 const face=await fonts('h1');assert.ok(face.length&&face.every(f=>f.familyName.startsWith('Be Vietnam Pro')),'Vietnamese heading at '+width);checks.push('Vietnamese heading uses local font at '+width);pages.push({...data,fonts:face});
 if([320,390,768,1101,1366,1440].includes(width))await screenshot('home-'+width,width===1440||width===390);
 if(width===1101)await check('Desktop menu fits at its smallest breakpoint',`Array.from(document.querySelectorAll('.catalogNav > a,.catalogNav > details')).every(e=>{const r=e.getBoundingClientRect();return r.x>=0&&r.right<=innerWidth;})`);
}
await visit('/');
for(const weight of [400,500,600,700]){
 await evaluate(`(()=>{const p=document.createElement('p');p.id='font-probe';p.style.fontWeight='${weight}';p.textContent='Tiếng Việt: ă â ê ô ơ ư đ Ắ Ễ Ộ Ở Ữ ỵ — Học phí 3.500.000 ₫';document.querySelector('main').append(p);})()`);
 await evaluate('document.fonts.ready.then(()=>true)');
 const rendered=await fonts('#font-probe');assert.ok(rendered.length&&rendered.every(f=>f.familyName.startsWith('Be Vietnam Pro')),'Vietnamese and currency coverage at '+weight+': '+JSON.stringify(rendered));checks.push('Vietnamese and currency use local font at weight '+weight);await evaluate('document.querySelector("#font-probe").remove()');
}
await check('Menu uses SVG chevrons','!!document.querySelector(".navChevron path")&&!document.querySelector(".catalogHeader").textContent.includes("⌄")');
await click('.desktopNav details summary');await check('Desktop dropdown opens','!!document.querySelector(".desktopNav details[open]")');
await send('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await send('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
await check('Escape closes dropdown and returns focus','!document.querySelector("details[open]")&&document.activeElement.tagName==="SUMMARY"');
await click('.desktopNav details summary');await click('h1');await check('Outside click closes dropdown','!document.querySelector("details[open]")');
await visit('/khoa-hoc?audience=SECONDARY');await check('Filtered navigation remains active','!!document.querySelector(".catalogNav a[aria-current][href*=SECONDARY]")&&document.querySelector("select[name=audience]").value==="SECONDARY"');
await visit('/',390);await click('.menuButton');await check('Mobile menu opens with no overflow','document.querySelector(".menuButton").getAttribute("aria-expanded")==="true"&&document.documentElement.scrollWidth<=innerWidth');await screenshot('mobile-menu-390');
await click('.catalogMobileNav details summary');await check('Mobile submenu opens','!!document.querySelector(".catalogMobileNav details[open]")');
await click('.menuButton');await check('Mobile menu closes','!document.querySelector(".catalogMobileNav")');
await visit('/khoa-hoc/mg2');await evaluate('scrollTo({top:650,behavior:"instant"})');await check('Sticky enrollment clears header by 24px','document.querySelector(".detailAside").getBoundingClientRect().top>=document.querySelector(".catalogHeader").getBoundingClientRect().bottom+23');await screenshot('sticky-detail-1440');
for(const width of [390,1440]){
 await visit('/tu-van',width);
 await check('Consultation fields use 16px and regular weight at '+width,'Array.from(document.querySelectorAll(".consultationForm input:not([type=checkbox]),.consultationForm select,.consultationForm textarea")).filter(e=>e.getBoundingClientRect().height>0).every(e=>getComputedStyle(e).fontSize==="16px"&&getComputedStyle(e).fontWeight==="400")');
 for(const selector of ['h1','label','input[name=fullName]']){const face=await fonts(selector);assert.ok(face.length&&face.every(f=>f.familyName.startsWith('Be Vietnam Pro')),'Local form font '+selector+' '+width);}
 checks.push('Consultation Vietnamese heading/label/input use local font at '+width);
 await screenshot('consultation-'+width);
}
assert.deepEqual(errors,[]);checks.push('No browser runtime exceptions');
fs.writeFileSync(path.join(output,'report.json'),JSON.stringify({checkedAt:new Date().toISOString(),base,readOnly:true,checks,pages,errors,limitations:'Chrome emulation only; no physical phone or form submission.'},null,2));
console.log(JSON.stringify({checks:checks.length,passed:true,output,pages:pages.map(({width,headingSize,headerHeight})=>({width,headingSize,headerHeight}))},null,2));
}catch(error){
 const diagnostic={base,error:error.message,errors,checks,consoleMessages};
 try{diagnostic.page=await evaluate('({url:location.href,ready:document.readyState,header:!!document.querySelector(".catalogHeader"),offset:document.documentElement.style.getPropertyValue("--header-height"),scripts:Array.from(document.scripts).filter(s=>s.src).map(s=>s.src),devError:Array.from(document.querySelector("nextjs-portal")?.shadowRoot?.querySelectorAll("h1,h2,p,[data-nextjs-dialog-header],pre")||[]).map(e=>e.textContent).join("\\n").slice(0,8000)})');}catch{}
 fs.writeFileSync(path.join(output,'failure.json'),JSON.stringify(diagnostic,null,2));throw error;
}finally{ws?.close();if(chrome?.pid&&chrome.exitCode===null)try{execFileSync('taskkill.exe',['/PID',String(chrome.pid),'/T','/F'],{stdio:'ignore',windowsHide:true});}catch{}}})().catch(e=>{console.error(e.stack);process.exitCode=1;});
