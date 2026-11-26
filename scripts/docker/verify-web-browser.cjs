const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{spawn}=require('node:child_process');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'docs/docker/screenshots');fs.mkdirSync(out,{recursive:true});
const checks=[],errors=[],requests=[],pending=new Map();let browser,socket,id=0;
const delay=ms=>new Promise(r=>setTimeout(r,ms)),keepAlive=setInterval(()=>{},1000);
function send(method,params={}){return new Promise((resolve,reject)=>{const key=++id;pending.set(key,{resolve,reject});socket.send(JSON.stringify({id:key,method,params}));});}
async function evaluate(expression){const r=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.text);return r.result.value;}
async function wait(expression){for(let i=0;i<100;i++){if(await evaluate(expression))return;await delay(100);}throw Error('Timeout: '+expression);}
async function check(name,expression){assert.ok(await evaluate(expression),name);checks.push(name);}
async function visit(url){await send('Page.navigate',{url});await wait('document.readyState==="complete"&&!!document.querySelector("main,form")');await delay(500);}
async function shot(name){const r=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:false});fs.writeFileSync(path.join(out,name+'.png'),Buffer.from(r.data,'base64'));}
async function run(){
 browser=spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=19244','--user-data-dir='+path.join(root,'tmp/dck4-chrome-profile'),'about:blank'],{windowsHide:true,stdio:'ignore'});
 browser.on('error',e=>errors.push(e.message));let target;
 for(let i=0;i<100;i++){try{target=(await(await fetch('http://127.0.0.1:19244/json/list')).json()).find(t=>t.type==='page');if(target)break;}catch{}await delay(100);}
 if(!target)throw Error('Chrome unavailable');socket=new WebSocket(target.webSocketDebuggerUrl);
 socket.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);if(p)m.error?p.reject(Error(m.error.message)):p.resolve(m.result);}else if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.exception?.description||m.params.exceptionDetails.text);else if(m.method==='Network.requestWillBeSent')requests.push(m.params.request.url);};
 await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j;});await send('Runtime.enable');await send('Page.enable');await send('Network.enable');
 await send('Emulation.setDeviceMetricsOverride',{width:1440,height:900,deviceScaleFactor:1,mobile:false});
 await visit('http://localhost:15173');await check('Admin login renders','!!document.querySelector("input[type=password]")');await shot('dck4-admin-login');
 await check('Admin browser API reaches gateway','fetch("/api/v1/academic/public/catalog/COURSE").then(async r=>r.ok&&Array.isArray((await r.json()).items))');
 await visit('http://localhost:15173/admin/students');await check('Protected deep link restores login','!!document.querySelector("input[type=password]")');
 for(const width of [1440,390]){
  await send('Emulation.setDeviceMetricsOverride',{width,height:900,deviceScaleFactor:1,mobile:width<600});
  for(const route of ['/','/khoa-hoc','/tu-van']){
   await visit('http://localhost:13000'+route);await evaluate('document.fonts.ready.then(()=>true)');
   await check('Website '+route+' fits '+width,'document.documentElement.scrollWidth<=innerWidth');
   await check('Website '+route+' loads without backend-error UI '+width,'!!document.querySelector("main h1")&&!document.querySelector("main").innerText.includes("Chưa tải được thông tin.")');
   await shot('dck4-site-'+(route.slice(1)||'home')+'-'+width);
  }
 }
 await check('Browser catalog loads from Next BFF','fetch("/api/catalog?kind=COURSE").then(async r=>r.ok&&Array.isArray((await r.json()).items))');
 assert.equal(errors.length,0,'Uncaught browser errors');checks.push('No uncaught browser errors');
 assert.ok(!requests.some(u=>/^https?:\/\/(api-gateway|academic-service|auth-service)(:|\/)/.test(u)||u.startsWith('http://localhost:8080/')));checks.push('Browser never calls internal Docker names or old gateway port');
 fs.writeFileSync(path.join(root,'docs/docker/dck4-browser-validation.json'),JSON.stringify({checkedAt:new Date().toISOString(),status:'passed',browser:'Chrome headless',checks,errors,viewports:[1440,390],screenshots:'screenshots/dck4-*.png',limitations:['No physical phone testing.','Database empty: positive authenticated flows deferred to DCK5.']},null,2));console.log(JSON.stringify({status:'passed',checks:checks.length}));
}
run().catch(e=>{console.error(e.message);process.exitCode=1;}).finally(async()=>{if(socket?.readyState===WebSocket.OPEN){try{await Promise.race([send('Browser.close'),delay(1000)]);}catch{}socket.close();}if(browser&&!browser.killed)browser.kill();clearInterval(keepAlive);});
