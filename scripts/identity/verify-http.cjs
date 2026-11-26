const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto'),net=require('net'),{spawn,execFileSync}=require('child_process');
const {query,schema,root,runtimeEnvironment}=require('./database.cjs');process.chdir(root);
const db='lc_identity_http_'+Date.now(),children=[],logs=[],checks=[];
const bases={auth:'http://127.0.0.1:30181',academic:'http://127.0.0.1:30182',teaching:'http://127.0.0.1:30183',finance:'http://127.0.0.1:30184'};
const folder='storage/'+db;fs.mkdirSync(folder,{recursive:true});
async function free(port){await new Promise((resolve,reject)=>{const s=net.createServer();s.once('error',reject);s.listen(port,'127.0.0.1',()=>s.close(resolve));});}
async function request(service,route,token,branch=1,body,method){const r=await fetch(bases[service]+'/api/v1/'+service+route,{method:method??(body?'POST':'GET'),headers:{...(token?{Authorization:'Bearer '+token}:{}),'X-Branch-Id':String(branch),'Content-Type':'application/json'},...(body?{body:JSON.stringify(body)}:{}),signal:AbortSignal.timeout(15000)});const text=await r.text();let data;try{data=JSON.parse(text);}catch{data={text:text.slice(0,100)};}return {status:r.status,data};}
async function ok(name,service,route,token,branch=1){const r=await request(service,route,token,branch);assert.equal(r.status,200,name+': '+JSON.stringify(r.data));checks.push({name,status:'PASS'});return r.data;}
async function denied(name,service,route,token,branch=1){const r=await request(service,route,token,branch);assert.ok([403,404].includes(r.status),name+': '+r.status);checks.push({name,status:'PASS'});}
async function login(name){const r=await request('auth','/login',null,1,{username:name,password:'Demo@123456'});assert.equal(r.status,200,'login '+name+': '+JSON.stringify(r.data));assert.ok(r.data.token);checks.push({name:'login '+name,status:'PASS'});return r.data;}
(async()=>{
 let created=false;
 try{
  for(const port of [30181,30182,30183,30184])await free(port);
  query(null,'CREATE DATABASE `'+db+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;');created=true;
  query(db,schema());query(db,fs.readFileSync('docs/demo/seed-identity-full.sql','utf8'));
  const env={...runtimeEnvironment(db),JWT_SECRET:crypto.randomBytes(48).toString('base64'),AUTH_URL:bases.auth,ACADEMIC_URL:bases.academic,TEACHING_URL:bases.teaching,FINANCE_URL:bases.finance,SPRING_CONFIG_IMPORT:'optional:file:/nonexistent-identity.properties',SPRING_PROFILES_ACTIVE:'prod',DDL_AUTO:'validate',VNPAY_ENABLED:'false',PAYMENT_JOBS_ENABLED:'false',NOTIFICATION_JOBS_ENABLED:'false',NOTIFICATIONS_PUSH_ENABLED:'false',ACADEMIC_SEED_BRANCHES_ENABLED:'false',AUTH_BOOTSTRAP_ENABLED:'false',CORS_ALLOWED_ORIGINS:'http://localhost:5173',DB_POOL_SIZE:'3',DOCUMENT_STORAGE:path.resolve(folder,'documents')};
  for(const [service,port] of [['auth',30181],['academic',30182],['teaching',30183],['finance',30184]]){
   const target=service+'-service/target',jar=fs.readdirSync(target).find(f=>f.endsWith('.jar')&&!f.endsWith('.original'));
   assert.ok(jar,'Missing package for '+service);
   const log=fs.openSync(path.join(folder,service+'.log'),'w');logs.push(log);
   const child=spawn('java',['-Xmx192m','-Dlc.identity.http='+db,'-jar',path.resolve(target,jar),'--server.address=127.0.0.1','--server.port='+port,'--spring.jpa.hibernate.ddl-auto=validate','--debug=false','--logging.level.org.hibernate.SQL=OFF'],{env,cwd:root,windowsHide:true,stdio:['ignore',log,log]});children.push(child);
  }
  for(const [service,base] of Object.entries(bases)){
   let ready=false;for(let i=0;i<100;i++){if(children.some(c=>c.exitCode!==null))throw Error('Service exited; see '+folder);try{const r=await fetch(base+'/actuator/health',{signal:AbortSignal.timeout(1500)});if(r.ok){ready=true;break;}}catch{}await new Promise(r=>setTimeout(r,500));}
   assert.ok(ready,service+' startup timeout');
  }
  const sa=await login('superadmin'),admin=await login('admin1'),staff=await login('staff1'),teacher=await login('teacher1'),student=await login('hv0003');
  assert.equal(admin.personnelCode,'NS001');assert.equal(teacher.personnelCode,'GV007');
  const found=await ok('search personnel code','auth','/accounts?keyword=NS001',sa.token);assert.equal(found.totalElements,1);
  await denied('teacher cannot manage accounts','auth','/accounts',teacher.token);
  await denied('staff cannot edit employment','auth','/accounts/4/employment',staff.token);
  await denied('admin cannot read branch two','academic','/students?branchId=2',admin.token,2);
  await denied('staff cannot read branch two','academic','/students?branchId=2',staff.token,2);
  const own=await ok('teacher branch one classes','academic','/classes?branchId=1',teacher.token);assert.ok(own.content.length>0&&own.content.every(c=>c.teacherId===1));
  const second=await ok('same teacher branch two classes','academic','/classes?branchId=2',teacher.token,2);assert.ok(second.content.length>0&&second.content.every(c=>c.teacherId===1));
  const profile=await ok('student own code','academic','/student/me/profile',student.token);assert.equal(profile.studentCode,'HV0003');
  const grades=await ok('student published grades','teaching','/student/me/enrollments/3/grades',student.token);assert.equal(grades.totalElements,1);
  const publication=await ok('published source fingerprint','teaching','/exams/1/student-publication',sa.token);assert.equal(publication.draftChanged,false);
  const tuition=await ok('student tuition','finance','/student/me/tuitions',student.token);assert.equal(tuition.totalElements,1);
  await denied('student cannot access another tuition','finance','/student/me/tuitions/1',student.token);
  const paid=await login('hv0001');const receipts=await ok('student own receipts','finance','/student/me/receipts',paid.token);assert.equal(receipts.totalElements,1);
  const employment=await ok('staff employment version','auth','/accounts/5/employment',sa.token);
  const changed=await request('auth','/accounts/5/employment',sa.token,1,{version:employment.version,workStatus:'INACTIVE',hireDate:employment.hireDate,endDate:new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date()),reason:'HTTP verification'},'PATCH');assert.equal(changed.status,200,JSON.stringify(changed.data));
  const blocked=await request('auth','/login',null,1,{username:'staff2',password:'Demo@123456'});assert.equal(blocked.status,403);checks.push({name:'offboarded employee cannot log in',status:'PASS'});
  const assignees=await ok('offboarded employee excluded from lead assignment','academic','/leads/assignees?branchId=1',sa.token);assert.ok(assignees.every(a=>a.accountId!==5));
  const stale=await request('auth','/accounts/5/employment',sa.token,1,{version:employment.version,workStatus:'ACTIVE',hireDate:employment.hireDate,endDate:null,reason:'Stale update'},'PATCH');assert.equal(stale.status,409);checks.push({name:'stale employment update rejected',status:'PASS'});
  fs.writeFileSync('storage/identity-g456-http-verification.json',JSON.stringify({status:'PASS',database:db,realMySql:true,realServices:4,realModel:false,checks},null,2));console.log(JSON.stringify({status:'PASS',checks:checks.length,database:db}));
 }finally{
  for(const child of children)if(child.exitCode===null){
    if(process.platform==='win32')execFileSync('taskkill.exe',['/PID',String(child.pid),'/T','/F'],{stdio:'ignore',windowsHide:true});
    else child.kill();
  }
  await Promise.all(children.map(child=>child.exitCode!==null?Promise.resolve():new Promise(resolve=>child.once('exit',resolve))));
  for(const fd of logs)fs.closeSync(fd);
  if(created)query(null,'DROP DATABASE `'+db+'`;');
 }
})().catch(e=>{console.error(e.message);process.exitCode=1;});
