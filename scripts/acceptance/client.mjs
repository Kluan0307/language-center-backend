import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
export const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
export const jsonRead=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
export const dir=path.resolve(process.env.ACCEPTANCE_RUN_DIR||fs.readFileSync(path.join(root,'storage/acceptance/latest.txt'),'utf8').trim());
assert.ok(dir.startsWith(path.join(root,'storage/acceptance')+path.sep),'Run directory outside acceptance workspace');
export const runtime=jsonRead(path.join(dir,'runtime.json'));
assert.match(runtime.database??'',/^lc_rehearsal_[a-zA-Z0-9_]+$/,'Only isolated rehearsal databases allowed');
export const expected=jsonRead(path.join(dir,'expected.json'));
export const base='http://127.0.0.1:'+runtime.basePort+'/api/v1';
export const privateFile=path.join(dir,'private-session.json');
export const stateFile=path.join(dir,'fixture.json');
export const loadState=()=>fs.existsSync(stateFile)?jsonRead(stateFile):{prefix:runtime.prefix,today:new Date().toLocaleDateString('en-CA',{timeZone:'Asia/Ho_Chi_Minh'})};
export const saveState=s=>fs.writeFileSync(stateFile,JSON.stringify(scrub(s),null,2));
export const secrets=fs.existsSync(privateFile)?jsonRead(privateFile):{accounts:{}};
export const saveSecrets=()=>fs.writeFileSync(privateFile,JSON.stringify(secrets,null,2));
export const uuid=()=>crypto.randomUUID();
export function scrub(v){if(Array.isArray(v))return v.map(scrub);if(v&&typeof v==='object')return Object.fromEntries(Object.entries(v).filter(([k])=>!/(token|password)$/i.test(k)).map(([k,x])=>[k,scrub(x)]));return v;}
export function suite(phase){const result={phase,runId:runtime.runId,database:runtime.database,startedAt:new Date().toISOString(),transport:'Real HTTP through isolated gateway; MySQL V013',expected:[],checks:[],status:'RUNNING'};const file=path.join(dir,phase+'.json');if(fs.existsSync(file)){const prior=jsonRead(file);if(phase==='g17'||prior.status==='PASSED')throw Error('Phase already recorded; prepare a fresh isolated run to repeat it.');fs.copyFileSync(file,path.join(dir,phase+'-failed-attempt-'+Date.now()+'.json'));}const save=()=>fs.writeFileSync(file,JSON.stringify(result,null,2));save();return{result,save,async check(id,description,wanted,action){assert.ok(!result.checks.some(x=>x.id===id),'Duplicate check ID');result.expected.push({id,description,expected:scrub(wanted)});save();const started=Date.now();try{const actual=await action();assert.deepEqual(actual,wanted,id+': '+description);result.checks.push({id,description,expected:scrub(wanted),actual:scrub(actual),passed:true,elapsedMs:Date.now()-started});console.log('PASS '+phase+' '+id);save();return actual;}catch(e){result.checks.push({id,description,expected:scrub(wanted),passed:false,error:e.message,elapsedMs:Date.now()-started});result.status='FAILED';save();throw e;}},finish(){assert.equal(result.checks.length,expected.requiredChecks[phase],'Required acceptance coverage incomplete');result.status=result.checks.every(x=>x.passed)?'PASSED':'FAILED';result.finishedAt=new Date().toISOString();save();console.log(phase+' '+result.checks.length+' real checks '+result.status);}};}
export async function api(route,actor,method='GET',body,options={}){const token=typeof actor==='string'?actor:actor?.token;const headers={...(token?{Authorization:'Bearer '+token}:{}),...(options.branch?{'X-Branch-Id':String(options.branch)}:{})};const multipart=body instanceof FormData;if(body&&!multipart)headers['Content-Type']='application/json';const started=Date.now();let response;try{response=await fetch(base+route,{method,headers,body:body?(multipart?body:JSON.stringify(body)):undefined,signal:AbortSignal.timeout(options.timeout??20000)});}catch(e){fs.appendFileSync(path.join(dir,'http.jsonl'),JSON.stringify({at:new Date().toISOString(),method,route,networkError:e.name})+'\n');throw e;}
let data;if(options.binary){const b=Buffer.from(await response.arrayBuffer());data={bytes:b.length,sha256:crypto.createHash('sha256').update(b).digest('hex')};}else{const text=await response.text();try{data=JSON.parse(text);}catch{data=text;}}
fs.appendFileSync(path.join(dir,'http.jsonl'),JSON.stringify({at:new Date().toISOString(),method,route,request:multipart?'multipart fixture':scrub(body),status:response.status,requestId:response.headers.get('x-request-id'),durationMs:Date.now()-started,response:scrub(data)})+'\n');return{status:response.status,data};}
export async function ok(route,actor,method='GET',body,options){const r=await api(route,actor,method,body,options);assert.equal(r.status,200,route+' '+JSON.stringify(scrub(r.data)));return r.data;}
export async function login(username,password){const data=await ok('/auth/login',null,'POST',{username,password});return{username,password,token:data.token};}
export async function account(role,branch,suffix,sa){const username=runtime.prefix+'_'+suffix,password=process.env.ACCEPTANCE_DEMO_PASSWORD||('Demo-'+crypto.randomBytes(12).toString('hex')+'!');const record=await ok('/auth/accounts',sa,'POST',{username,password,role,fullName:runtime.prefix+' '+suffix,branchId:branch});let initial=await login(username,password);const changed=password+'9';assert.equal((await api('/auth/me/password',initial,'POST',{currentPassword:password,newPassword:changed,confirmPassword:changed})).status,204);const user={...record,...await login(username,changed)};secrets.accounts[suffix]=user;saveSecrets();return user;}
export async function ready(){
  const deadline=Date.now()+90000;
  while(Date.now()<deadline){
    const states=await Promise.all([0,1,2,3,4].map(async offset=>{
      try{const response=await fetch('http://127.0.0.1:'+(runtime.basePort+offset)+'/actuator/health/readiness',{signal:AbortSignal.timeout(2000)});return response.ok&&(await response.json()).status==='UP';}catch{return false;}
    }));
    if(states.every(Boolean))return;
    await new Promise(r=>setTimeout(r,500));
  }
  throw Error('All five isolated services must be ready before acceptance starts');
}
