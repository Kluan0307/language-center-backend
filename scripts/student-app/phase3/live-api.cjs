const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const dir=fs.readFileSync('storage/student-phase3-latest.txt','utf8').trim(),run=JSON.parse(fs.readFileSync(path.join(dir,'runtime.json'),'utf8')),secrets=JSON.parse(fs.readFileSync(path.join(dir,'secrets.json'),'utf8'));const base='http://127.0.0.1:'+run.basePort+'/api/v1';const report={database:run.database,status:'RUNNING',checks:[]};
const save=()=>fs.writeFileSync(path.join(dir,'api-checks.json'),JSON.stringify(report,null,2));
async function api(route,token,method='GET',body,headers={}){const r=await fetch(base+route,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{}),...headers},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(20000)});let data;const text=await r.text();try{data=JSON.parse(text)}catch{data=text}return {status:r.status,data};}
async function check(name,expected,action){const r=await action();assert.equal(r.status,expected,name);report.checks.push({name,status:'PASS',http:expected});save();return r.data;}
async function login(username,password){return check('login '+username,200,()=>api('/auth/login',null,'POST',{username,password}));}
(async()=>{try{
for(let n=0;n<90;n++){try{const health=await Promise.all([0,1,2,3,4].map(async offset=>(await fetch('http://127.0.0.1:'+(run.basePort+offset)+'/actuator/health',{signal:AbortSignal.timeout(2000)})).status));if(health.every(status=>status===200))break;}catch{}if(n===89)throw Error('Services not ready');await new Promise(r=>setTimeout(r,500));}
let sa=(await login(secrets.username,secrets.password)).token;
const branchA=await check('create branch A',200,()=>api('/academic/branches',sa,'POST',{code:'G3A',name:'G3 A',phone:'0900000001',address:'A'}));
const branchB=await check('create branch B',200,()=>api('/academic/branches',sa,'POST',{code:'G3B',name:'G3 B',phone:'0900000002',address:'B'}));
const students=[];for(let i=0;i<4;i++)students.push(await check('create student '+i,200,()=>api('/academic/students',sa,'POST',{fullName:'G3 Student '+i,phone:'090000001'+i,branchId:i===3?branchB.id:branchA.id,dateOfBirth:'2004-01-01',gender:'MALE',email:'g3-'+i+'@example.test'})));
const initial={requestId:crypto.randomUUID(),studentId:students[0].id,expectedStudentVersion:students[0].version,username:'g3-student',temporaryPassword:secrets.password,confirmPassword:secrets.password};
let account=await check('provision student A',201,()=>api('/auth/student-accounts',sa,'POST',initial));
const replay=await check('replay provisioning',201,()=>api('/auth/student-accounts',sa,'POST',initial));assert.equal(replay.accountId,account.accountId);
await check('recover request result',200,()=>api('/auth/student-account-provisionings/'+initial.requestId,sa));
await check('different intent for linked student',409,()=>api('/auth/student-accounts',sa,'POST',{...initial,requestId:crypto.randomUUID(),username:'duplicate'}));
await check('same intent changed password',409,()=>api('/auth/student-accounts',sa,'POST',{...initial,temporaryPassword:'changed-pass-123',confirmPassword:'changed-pass-123'}));
await check('stale profile prevents account',409,()=>api('/auth/student-accounts',sa,'POST',{...initial,requestId:crypto.randomUUID(),studentId:students[2].id,expectedStudentVersion:99,username:'g3-stale'}));
const list=await check('no orphan from stale version',200,()=>api('/auth/accounts?keyword=g3-stale',sa));assert.equal(list.totalElements,0);
let stu=await login(initial.username,initial.temporaryPassword);assert.equal(stu.mustChangePassword,true);
await check('activation blocks Academic',403,()=>api('/academic/student/me/profile',stu.token));
const password='Activated-'+secrets.password;await check('change password',204,()=>api('/auth/me/password',stu.token,'POST',{currentPassword:initial.temporaryPassword,newPassword:password,confirmPassword:password}));
await check('old JWT revoked after activation',401,()=>api('/auth/me',stu.token));stu=await login(initial.username,password);assert.equal(stu.mustChangePassword,false);
const profile=await check('read own profile',200,()=>api('/academic/student/me/profile',stu.token));assert.equal(profile.studentId,students[0].id);assert.equal(profile.branch.id,branchA.id);assert.equal(profile.guardianPhone,undefined);
const spoof=await check('studentId/header cannot switch identity',200,()=>api('/academic/student/me/profile?studentId='+students[3].id,stu.token,'GET',undefined,{'X-Branch-Id':String(branchB.id)}));assert.equal(spoof.studentId,students[0].id);
for(const route of ['/academic/students','/academic/students/'+students[3].id,'/teaching/grades?examId=1','/finance/payments','/auth/teachers','/auth/accounts','/auth/student-accounts?studentId='+students[3].id])await check('student denied '+route,403,()=>api(route,stu.token));
await check('student cannot patch profile directly',403,()=>api('/auth/me/profile',stu.token,'PATCH',{email:'other@example.test'}));
await check('linked student cannot be deleted',409,()=>api('/academic/students/'+students[0].id,sa,'DELETE'));
const actors={};for(const role of ['ADMIN','STAFF','TEACHER']){const username='g3-'+role.toLowerCase();await check('create operator '+role,200,()=>api('/auth/accounts',sa,'POST',{username,password:secrets.password,fullName:'G3 '+role,role,branchId:branchA.id}));let a=await login(username,secrets.password);const newPass='Op-'+secrets.password;await check('activate operator '+role,204,()=>api('/auth/me/password',a.token,'POST',{currentPassword:secrets.password,newPassword:newPass,confirmPassword:newPass}));a=await login(username,newPass);actors[role]={username,password:newPass,token:a.token};}
await check('admin same branch manages',200,()=>api('/auth/student-accounts?studentId='+students[0].id,actors.ADMIN.token));
await check('admin other branch denied',404,()=>api('/auth/student-accounts?studentId='+students[3].id,actors.ADMIN.token));
await check('staff cannot provision',403,()=>api('/auth/student-accounts',actors.STAFF.token,'POST',{...initial,requestId:crypto.randomUUID(),studentId:students[2].id,username:'staff-illegal'}));
for(const role of ['ADMIN','STAFF'])await check('old web student list '+role,200,()=>api('/academic/students',actors[role].token));
for(const role of ['ADMIN','STAFF','TEACHER'])await check('operator denied self profile '+role,403,()=>api('/academic/student/me/profile',actors[role].token));
const concurrent=await Promise.all([0,1].map(i=>api('/auth/student-accounts',sa,'POST',{...initial,requestId:crypto.randomUUID(),studentId:students[1].id,username:'g3-race-'+i})));assert.deepEqual(concurrent.map(r=>r.status).sort(),[201,409]);report.checks.push({name:'concurrent provisioning one success one conflict',status:'PASS'});save();
account=await check('lock student',200,()=>api('/auth/student-accounts/'+account.accountId+'/status',sa,'PATCH',{active:false,version:account.version,reason:'Verified lock'}));
await check('locked JWT revoked',401,()=>api('/auth/me',stu.token));
const resetPassword='Reset-'+secrets.password;
account=await check('reset locked account',200,()=>api('/auth/student-accounts/'+account.accountId+'/reset-password',sa,'POST',{temporaryPassword:resetPassword,confirmPassword:resetPassword,version:account.version,reason:'Verified identity'}));assert.equal(account.active,false);
await check('reset does not unlock',401,()=>api('/auth/login',null,'POST',{username:initial.username,password:resetPassword}));
await check('stale management version',409,()=>api('/auth/student-accounts/'+account.accountId+'/status',sa,'PATCH',{active:true,version:0,reason:'Stale attempt'}));
account=await check('unlock account',200,()=>api('/auth/student-accounts/'+account.accountId+'/status',sa,'PATCH',{active:true,version:account.version,reason:'Verified unlock'}));
stu=await login(initial.username,resetPassword);assert.equal(stu.mustChangePassword,true);
const other={...initial,requestId:crypto.randomUUID(),studentId:students[3].id,username:'g3-other'};await check('provision student other branch',201,()=>api('/auth/student-accounts',sa,'POST',other));
const otherLogin=await login(other.username,other.temporaryPassword);await check('activate other student',204,()=>api('/auth/me/password',otherLogin.token,'POST',{currentPassword:other.temporaryPassword,newPassword:password,confirmPassword:password}));const otherActive=await login(other.username,password);const otherProfile=await check('second student gets own profile',200,()=>api('/academic/student/me/profile',otherActive.token));assert.equal(otherProfile.studentId,students[3].id);
fs.writeFileSync(path.join(dir,'fixture.json'),JSON.stringify({branchA,branchB,students,student:{username:initial.username,password:resetPassword,finalPassword:'Ui-'+secrets.password},operators:actors,account,saToken:sa},null,2));
report.status='PASS';console.log(JSON.stringify({status:report.status,checks:report.checks.length,database:run.database}));
}catch(e){report.status='FAIL';report.failure=e.message;console.log(JSON.stringify(report));process.exitCode=1;}finally{save();}})();

