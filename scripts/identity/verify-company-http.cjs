const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto'),net=require('net'),{spawn,execFileSync}=require('child_process');
const {query,dump,root,runtimeEnvironment}=require('./database.cjs');process.chdir(root);
const db='lc_identity_http_'+Date.now(),children=[],fds=[],checks=[];const dir='storage/'+db;fs.mkdirSync(dir,{recursive:true});
async function req(path,token,body,method){const r=await fetch('http://127.0.0.1:30181/api/v1/auth'+path,{method:method??(body?'POST':'GET'),headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(15000)});return {status:r.status,data:await r.json()};}
async function expected(name,path,token,status,body,method){const r=await req(path,token,body,method);assert.equal(r.status,status,name+': '+JSON.stringify(r.data));checks.push({name,status:'PASS'});return r.data;}
(async()=>{let created=false;try{
 for(const port of [30181,30182])await new Promise((resolve,reject)=>{const s=net.createServer();s.once('error',reject);s.listen(port,'127.0.0.1',()=>s.close(resolve));});
 query(null,'CREATE DATABASE `'+db+'`;');created=true;query(db,dump());
 const env={...runtimeEnvironment(db),JWT_SECRET:crypto.randomBytes(48).toString('base64'),AUTH_URL:'http://127.0.0.1:30181',ACADEMIC_URL:'http://127.0.0.1:30182',TEACHING_URL:'http://127.0.0.1:30183',FINANCE_URL:'http://127.0.0.1:30184',SPRING_CONFIG_IMPORT:'optional:file:/nonexistent-company.properties',SPRING_PROFILES_ACTIVE:'prod',NOTIFICATION_JOBS_ENABLED:'false',NOTIFICATIONS_PUSH_ENABLED:'false',ACADEMIC_SEED_BRANCHES_ENABLED:'false',CORS_ALLOWED_ORIGINS:'http://localhost:5173',DB_POOL_SIZE:'3'};
 for(const [service,port] of [['auth',30181],['academic',30182]]){const target=service+'-service/target',jar=fs.readdirSync(target).find(f=>f.endsWith('.jar'));const fd=fs.openSync(path.join(dir,service+'.log'),'w');fds.push(fd);children.push(spawn('java',['-Xmx192m','-Dlc.company.http='+db,'-jar',path.resolve(target,jar),'--server.address=127.0.0.1','--server.port='+port,'--spring.jpa.hibernate.ddl-auto=validate','--logging.level.org.hibernate.SQL=OFF','--debug=false'],{cwd:root,env,windowsHide:true,stdio:['ignore',fd,fd]}));}
 for(const port of [30181,30182]){let ready=false;for(let n=0;n<120;n++){if(children.some(c=>c.exitCode!==null))throw Error('Service exited; see '+dir);try{if((await fetch('http://127.0.0.1:'+port+'/actuator/health',{signal:AbortSignal.timeout(1000)})).ok){ready=true;break;}}catch{}await new Promise(r=>setTimeout(r,500));}assert.ok(ready,'Startup timeout');}
 const tokens={};for(const name of ['superadmin','admin1','admin2','staff1','staff2','teacher1','hv0001']){tokens[name]=(await expected('login '+name,'/login',null,200,{username:name,password:'123456'})).token;}
 const org=await expected('shared organization','/company',tokens.teacher1,200);assert.equal(org.units.length,9);assert.equal(org.titles.length,9);
 const directory=await expected('public directory','/company/directory?size=100',tokens.teacher1,200);assert.equal(directory.totalElements,19);assert.ok(directory.content.every(p=>!('password' in p)&&!('email' in p)&&!('phone' in p)&&!('username' in p)&&!('qualification' in p)));
 await expected('directory filter by unit','/company/directory?unitId='+org.units.find(u=>u.branchId===1).id,tokens.staff1,200);
 await expected('student denied directory','/company',tokens.hv0001,403);
 await expected('teacher denied organization write','/company/titles',tokens.teacher1,403,{name:'Forbidden',active:true});
 const title=await expected('superadmin title creation','/company/titles',tokens.superadmin,200,{name:'Smoke title',active:true});
 await expected('stale organization update','/company/titles/'+title.id,tokens.superadmin,409,{name:'Stale',active:true,version:99},'PUT');
 const ticket=await expected('teacher creates support request','/internal-support/tickets',tokens.teacher1,200,{category:'BUG',title:'Lịch dạy',description:'Kiểm tra phạm vi hỗ trợ',branchId:1});
 await expected('other employee cannot read request','/internal-support/tickets/'+ticket.id,tokens.staff2,404);
 await expected('foreign branch admin cannot read request','/internal-support/tickets/'+ticket.id,tokens.admin2,404);
 await expected('teacher cannot inspect queue','/internal-support/tickets?queue=true',tokens.teacher1,403);
 await expected('owner cannot resolve request','/internal-support/tickets/'+ticket.id+'/replies',tokens.teacher1,403,{version:ticket.version,message:'Resolve',status:'RESOLVED'});
 const changed=await expected('branch admin replies','/internal-support/tickets/'+ticket.id+'/replies',tokens.admin1,200,{version:ticket.version,message:'Đang kiểm tra',status:'IN_PROGRESS'});
 assert.equal(changed.ticket.status,'IN_PROGRESS');
 await expected('old version cannot overwrite reply','/internal-support/tickets/'+ticket.id+'/replies',tokens.teacher1,409,{version:ticket.version,message:'Old'});
  let browserResult;
  if(process.argv.includes('--settings')){
   const initial=await expected('own default preferences','/settings/preferences',tokens.superadmin,200);assert.equal(initial.pageSize,10);
   await expected('student settings denied','/settings/preferences',tokens.hv0001,403);
   await expected('admin cannot edit company profile','/settings/company',tokens.admin1,403,{version:0,name:'Denied'},'PUT');
   await expected('save personal preferences','/settings/preferences',tokens.superadmin,200,{version:initial.version,compactTables:true,pageSize:20,calendarView:'day',defaultBranchId:null},'PUT');
   await expected('stale personal settings denied','/settings/preferences',tokens.superadmin,409,{version:initial.version,compactTables:false,pageSize:10,calendarView:'week',defaultBranchId:null},'PUT');
   const independent=await expected('other account preferences isolated','/settings/preferences',tokens.staff1,200);assert.equal(independent.pageSize,10);
   await expected('foreign branch preference denied','/settings/preferences',tokens.teacher1,403,{version:0,compactTables:false,pageSize:10,calendarView:'week',defaultBranchId:3},'PUT');
   const profile=await expected('public company profile','/settings/company',tokens.superadmin,200);
   await expected('save company public information','/settings/company',tokens.superadmin,200,{version:profile.version,name:'S5 Center',address:'Demo address',email:'contact@example.com',phone:'0900000000',logoUrl:null},'PUT');
   const visible=await expected('teacher sees published company name','/settings/company',tokens.teacher1,200);assert.equal(visible.name,'S5 Center');
  }
  if(process.argv.includes('--browser')){browserResult=await require('./verify-company-browser.cjs')(tokens.superadmin,dir);checks.push(...browserResult.checks);}
 fs.writeFileSync(process.argv.includes('--settings')?'storage/company-s45-http.json':'storage/company-s123-http.json',JSON.stringify({status:'PASS',realMySql:true,services:2,checks,browser:browserResult},null,2));console.log(JSON.stringify({status:'PASS',checks:checks.length}));
}finally{for(const c of children)if(c.exitCode===null)try{execFileSync('taskkill.exe',['/PID',String(c.pid),'/T','/F'],{stdio:'ignore',windowsHide:true});}catch{}await Promise.all(children.map(c=>c.exitCode!==null?Promise.resolve():new Promise(r=>c.once('exit',r))));for(const fd of fds)fs.closeSync(fd);if(created)query(null,'DROP DATABASE `'+db+'`;');}})().catch(e=>{console.error(e.message);process.exitCode=1;});
