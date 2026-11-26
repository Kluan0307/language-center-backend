const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{spawn}=require('node:child_process');
const base='http://127.0.0.1:3010',output=path.resolve(__dirname,'../../tmp/website-verification');fs.mkdirSync(output,{recursive:true});
const checks=[],errors=[];let browser,socket,id=0;const pending=new Map();
const delay=ms=>new Promise(r=>setTimeout(r,ms));
function send(method,params={}){return new Promise((resolve,reject)=>{const key=++id;pending.set(key,{resolve,reject});socket.send(JSON.stringify({id:key,method,params}));});}
async function evaluate(expression){const result=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(result.exceptionDetails)throw Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;}
async function wait(expression){for(let i=0;i<100;i++){if(await evaluate(expression))return;await delay(100);}throw Error('Timeout: '+expression);}
async function check(name,expression){assert.ok(await evaluate(expression),name);checks.push(name);}
async function shot(name){const result=await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:false});fs.writeFileSync(path.join(output,name+'.png'),Buffer.from(result.data,'base64'));}
async function visit(route){await send('Page.navigate',{url:base+route});await wait('document.readyState === "complete" && !!document.querySelector("h1")');await delay(200);}
async function run(){
 for(const route of ['/','/chuong-trinh','/chuong-trinh/luyen-thi-ielts','/gioi-thieu','/chi-nhanh','/tu-van?chuong-trinh=luyen-thi-ielts','/chinh-sach-bao-mat','/robots.txt','/sitemap.xml','/icon.svg']){const response=await fetch(base+route);assert.equal(response.status,200,route);checks.push('HTTP 200 '+route);}
 assert.equal((await fetch(base+'/chuong-trinh/does-not-exist')).status,404);checks.push('unknown program returns 404');
 assert.match(await(await fetch(base+'/robots.txt')).text(),/Disallow: \/\s/);checks.push('draft website not indexable');
 const home=await(await fetch(base)).text();assert.match(home,/lang="vi"/);assert.ok(!home.includes('Create Next App'));checks.push('Vietnamese metadata and starter content removed');
 browser=spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=19225','--user-data-dir='+path.join(output,'chrome-profile'),'about:blank'],{windowsHide:true,stdio:'ignore'});
 browser.on('error',error=>errors.push(error.message));let target;
 for(let i=0;i<100;i++){try{target=(await(await fetch('http://127.0.0.1:19225/json/list')).json()).find(t=>t.type==='page');if(target)break;}catch{}await delay(100);}
 if(!target)throw Error('Chrome unavailable');socket=new WebSocket(target.webSocketDebuggerUrl);socket.onmessage=e=>{const msg=JSON.parse(e.data);if(msg.id){const p=pending.get(msg.id);pending.delete(msg.id);if(p)msg.error?p.reject(Error(msg.error.message)):p.resolve(msg.result);}else if(msg.method==='Runtime.exceptionThrown')errors.push(msg.params.exceptionDetails.exception?.description||msg.params.exceptionDetails.text);};await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j;});
 await send('Runtime.enable');await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});await visit('/');
 await evaluate('Array.from(document.images).forEach(i=>i.loading="eager")');
 await wait('Array.from(document.images).every(i=>i.complete && i.naturalWidth>0)');await shot('home-desktop');
 await check('home has one heading and four program cards','document.querySelectorAll("h1").length===1 && document.querySelectorAll(".programCard").length===4');
 await check('desktop fits viewport','document.documentElement.scrollWidth <= innerWidth');
 await evaluate(`document.querySelector('#learning-goal').value='luyen-thi-ielts';document.querySelector('#learning-goal').form.requestSubmit()`);
 await wait('location.pathname === "/chuong-trinh" && document.querySelectorAll(".programCard").length === 1');
 await check('finder filters the intended program','document.body.innerText.includes("Luyện thi IELTS") && location.search.includes("luyen-thi-ielts")');
 await visit('/chuong-trinh/luyen-thi-ielts');await check('detail has program-specific metadata','document.title.startsWith("Luyện thi IELTS")');
 await evaluate(`Array.from(document.querySelectorAll('a')).find(a=>a.href.includes('/tu-van?chuong-trinh=')).click()`);await wait('location.pathname === "/tu-van" && document.body.innerText.includes("Chương trình bạn quan tâm")');
 await check('consultation preserves program context','document.body.innerText.includes("Luyện thi IELTS") && location.search.includes("luyen-thi-ielts")');
 await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});await visit('/');await wait('document.images[0].complete');await shot('home-mobile');
 await check('mobile fits viewport','document.documentElement.scrollWidth <= innerWidth');
 await evaluate(`document.querySelector('[aria-controls="mobile-navigation"]').click()`);await wait('document.querySelector("#mobile-navigation") !== null');checks.push('mobile menu opens');
 await evaluate(`document.querySelector('#mobile-navigation a[href="/chuong-trinh"]').click()`);await wait('location.pathname === "/chuong-trinh" && !document.querySelector("#mobile-navigation")');checks.push('mobile menu navigates and closes');
 await shot('programs-mobile');
 await send('Emulation.setDeviceMetricsOverride',{width:320,height:720,deviceScaleFactor:1,mobile:true});await visit('/');await check('home fits 320px','document.documentElement.scrollWidth <= innerWidth');
 await visit('/chuong-trinh/luyen-thi-toeic');await check('program detail fits 320px','document.documentElement.scrollWidth <= innerWidth');await shot('detail-mobile');
 await visit('/chi-nhanh');await check('unconfirmed addresses are not fabricated','document.body.innerText.includes("đang được cập nhật")');
 assert.equal(errors.length,0,errors.join('\n'));checks.push('no uncaught runtime exceptions');
}
run().then(()=>{fs.writeFileSync(path.join(output,'result.json'),JSON.stringify({status:'passed',checks,errors},null,2));console.log(JSON.stringify({status:'passed',count:checks.length,checks,output},null,2));}).catch(error=>{fs.writeFileSync(path.join(output,'result.json'),JSON.stringify({status:'failed',error:error.stack,checks,errors},null,2));console.error(error.stack);process.exitCode=1;}).finally(async()=>{if(socket?.readyState===WebSocket.OPEN){try{await Promise.race([send('Browser.close'),delay(1000)]);}catch{}socket.close();}if(browser&&!browser.killed)browser.kill();});
