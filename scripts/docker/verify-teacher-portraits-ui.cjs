const {fs,path,root,assert}=require('./data-common.cjs'),{spawn}=require('child_process');
const dir=path.join(root,'storage/teacher-portraits'),pending=new Map();let browser,socket,id=0;
const delay=ms=>new Promise(r=>setTimeout(r,ms));
const send=(method,params={})=>new Promise((resolve,reject)=>{const seq=++id;pending.set(seq,{resolve,reject});socket.send(JSON.stringify({id:seq,method,params}));});
async function evaluate(expression){const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.text);return r.result.value;}
async function main(){
 browser=spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=19256','--user-data-dir='+path.join(dir,'chrome-profile'),'about:blank'],{windowsHide:true,stdio:'ignore'});
 let target;for(let i=0;i<100;i++){try{target=(await(await fetch('http://127.0.0.1:19256/json/list')).json()).find(t=>t.type==='page');if(target)break;}catch{}await delay(100);}assert.ok(target);
 socket=new WebSocket(target.webSocketDebuggerUrl);await new Promise(r=>socket.addEventListener('open',r,{once:true}));socket.addEventListener('message',e=>{const m=JSON.parse(e.data);if(m.id&&pending.has(m.id)){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(Error(m.error.message)):p.resolve(m.result);}});await send('Page.enable');
 for(const width of [1440,390]){await send('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width<500});await send('Page.navigate',{url:'http://localhost:13000/giao-vien'});let loaded=false;for(let i=0;i<120;i++){loaded=await evaluate('document.readyState==="complete" && document.querySelectorAll("img[src*=cloudinary]").length===8');if(loaded)break;await delay(100);}assert.ok(loaded,'Eight teacher images missing in UI');
 await evaluate('Promise.all([...document.querySelectorAll("img[src*=cloudinary]")].map(i=>{i.loading="eager";return i.decode().catch(()=>{});})).then(()=>true)');assert.equal(await evaluate('[...document.querySelectorAll("img[src*=cloudinary]")].filter(i=>i.complete&&i.naturalWidth>0).length'),8);
 assert.equal(await evaluate('document.querySelectorAll(".teacherImageNote").length'),8,'Sample photo labels missing');assert.ok(await evaluate('[...document.querySelectorAll(".teacherPortrait img")].every(i=>getComputedStyle(i).objectPosition==="50% 0%")'),'Portrait framing not applied');
 const shot=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});fs.writeFileSync(path.join(root,'docs/docker/screenshots/teacher-portraits-'+width+'.png'),Buffer.from(shot.data,'base64'));
 }
 const reportPath=path.join(root,'docs/docker/teacher-portraits.json'),report=JSON.parse(fs.readFileSync(reportPath,'utf8'));report.checks=[...new Set([...report.checks,'Chrome desktop 1440px and mobile 390px: eight images loaded, illustration labels present, top portrait framing applied'])];fs.writeFileSync(reportPath,JSON.stringify(report,null,2));
 console.log('Eight teacher portraits and sample labels rendered at 1440px and 390px.');
}
main().catch(e=>{console.error(e.message);process.exitCode=1;}).finally(async()=>{if(socket?.readyState===WebSocket.OPEN){try{await Promise.race([send('Browser.close'),delay(1000)]);}catch{}socket.close();}if(browser&&!browser.killed)browser.kill();});
