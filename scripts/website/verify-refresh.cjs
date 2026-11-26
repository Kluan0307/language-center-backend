const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const {query,dump,root}=require('../identity/database.cjs');
const {withServices}=require('./local-services.cjs');
const {checks:relationalChecks}=require('../identity/refresh-validation.cjs');
const browser=require('./verify-refresh-browser.cjs');
const {repair}=require('./repair-demo-publications.cjs');
const database='lc_identity_http_'+Date.now(),folder=path.join(root,'storage',database),checks=[];
const ui7=process.argv.includes('--ui-u7'),browserName=process.env.UI_BROWSER==='edge'?'edge':'chrome';
const sourceSnapshot=dump(),sourceHash=crypto.createHash('sha256').update(sourceSnapshot).digest('hex');
fs.mkdirSync(folder,{recursive:true});
function pass(name){checks.push(name);}
(async()=>{let created=false;try{
 query(null,'CREATE DATABASE `'+database+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');created=true;
 query(database,sourceSnapshot);
 const result=await withServices({database,services:['auth','academic','teaching','finance','gateway'],folder,secret:crypto.randomBytes(48).toString('base64'),websiteKey:'d7-local-private-form-key-for-fixture-only'},async api=>{
  async function ok(name,service,route,user,branch=1){const r=await api.request(service,route,{token:user?.token,branch});assert.equal(r.status,200,name+': '+r.status+' '+JSON.stringify(r.data));pass(name);return r.data;}
  async function denied(name,service,route,user,branch=1){const r=await api.request(service,route,{token:user.token,branch});assert.ok([403,404].includes(r.status),name+': '+r.status);pass(name);}
  const sa=await api.login('superadmin'),admin1=await api.login('admin1'),admin2=await api.login('admin2'),staff=await api.login('staff1'),teacher=await api.login('teacher1'),otherTeacher=await api.login('teacher2'),student=await api.login('student1'),paidStudent=await api.login('student5');pass('Eight retained sample accounts log in without password changes');
  const fingerprints=await repair(api,database,{apply:true});pass('All '+fingerprints.checked+' grade fingerprints match teaching-service; scores unchanged');
  for(const [kind,count] of [['PROGRAM',4],['BRANCH',4],['COURSE',20],['CLASS',4]]){const r=await ok('Public '+kind,'academic','/public/catalog/'+kind+'?size=100');assert.equal(r.items.length,count);assert.equal(new Set(r.items.map(x=>x.slug)).size,count);}
  const teachers=await ok('Published teacher directory','academic','/public/teachers?size=100');assert.equal(teachers.items.length,8);assert.ok(teachers.items.every(t=>!t.qualification));pass('Unreviewed qualifications remain private');
  for(const code of ['PRESCHOOL','PRIMARY','SECONDARY','HIGH_SCHOOL','ADULT']){
   const pub=await ok('Public audience '+code,'academic','/public/catalog/COURSE?size=100&audience='+code);
   const managed=await ok('Assistant scope '+code,'academic','/assistant-query/courses?scopeMode=WHOLE_SYSTEM&size=20&status=OPEN&audienceCode='+code,sa,null);
   assert.deepEqual(pub.items.map(c=>c.code).sort(),managed.data.content.map(c=>c.code).sort());pass('Website and assistant distinct course sets agree for '+code);
  }
  const combined=await ok('Combined IELTS and THCS','academic','/public/catalog/COURSE?program=ielts&audience=SECONDARY&size=100');assert.equal(combined.items.length,2);
  const count=await ok('Distinct IELTS count','academic','/assistant-query/center-statistics?scopeMode=WHOLE_SYSTEM&metric=COURSES_COUNT&programCode=IELTS&audienceCode=SECONDARY',sa,null);assert.equal(count.data.value,2);
  const local1=await ok('Admin1 assigned branch courses','academic','/courses?branchId=1&size=100',admin1,1);assert.equal(local1.totalElements,17);
  const local3=await ok('Admin2 assigned branch courses','academic','/courses?branchId=3&size=100',admin2,3);assert.equal(local3.totalElements,17);
  await denied('Admin1 cannot read branch3 students','academic','/students?branchId=3',admin1,3);
  await denied('Staff cannot edit website CMS','academic','/website/publications/COURSE',staff);
  await denied('Teacher cannot edit website CMS','academic','/website/publications/COURSE',teacher);
  await denied('Teacher2 cannot read branch3 classes','academic','/classes?branchId=3',otherTeacher,3);
  const own=await ok('Teacher own classes','academic','/classes?branchId=1&size=100',teacher);assert.ok(own.content.length>0&&own.content.every(c=>c.teacherId===teacher.profileId));
  const profile=await ok('Relinked student profile','academic','/student/me/profile',student);assert.equal(profile.studentCode,'HV0401');assert.equal(student.profileId,401);
  const registrations=await ok('Student own classes','academic','/student/me/enrollments',student);assert.equal(registrations.totalElements,1);const ownEnrollment=registrations.content[0];assert.equal(ownEnrollment.classification.programCode,'GE');assert.ok(ownEnrollment.classification.audiences.some(a=>a.code==='PRESCHOOL'));
  const paidEnrollments=await ok('Student5 current and past enrollments','academic','/student/me/enrollments',paidStudent);assert.equal(paidEnrollments.totalElements,2);
  for(const e of paidEnrollments.content){const grades=await ok('Published grades for enrollment '+e.id,'teaching','/student/me/enrollments/'+e.id+'/grades',paidStudent);assert.ok(grades.totalElements>0);}
  const from='2026-09-01',to='2026-12-01';
  await ok('Student attendance','academic','/student/me/enrollments/'+ownEnrollment.id+'/attendance',student);
  await ok('Student schedule','academic','/student/me/schedule?from=2026-11-01&to=2026-11-30',student);
  await denied('Student cannot read another enrollment','academic','/student/me/enrollments/'+paidEnrollments.content[0].id,student);
  const tuitions=await ok('Student5 own tuition','finance','/student/me/tuitions',paidStudent);assert.equal(tuitions.totalElements,2);
  const receipts=await ok('Student5 own receipts','finance','/student/me/receipts',paidStudent);assert.equal(receipts.totalElements,2);
  await denied('Student1 cannot read Student5 receipt','finance','/student/me/receipts/'+receipts.content[0].id,student);
  const exam=Number(query(database,'SELECT MIN(id) FROM exams'));
  const publication=await ok('Grade source publication fingerprint','teaching','/exams/'+exam+'/student-publication',sa);assert.equal(publication.draftChanged,false);
  const gatewayCatalog=await fetch(api.bases.gateway+'/api/v1/academic/public/catalog/COURSE?audience=PRESCHOOL');assert.equal(gatewayCatalog.status,200);assert.equal((await gatewayCatalog.json()).items.length,2);pass('Gateway routes public catalog without requiring a management session');
  const gatewayPrivate=await fetch(api.bases.gateway+'/api/v1/academic/website/publications/COURSE');assert.equal(gatewayPrivate.status,401);pass('Gateway preserves upstream authentication for CMS');
  const browserResult=await browser({bases:{...api.bases,academic:api.bases.gateway},folder,websiteKey:'d7-local-private-form-key-for-fixture-only',extended:ui7});
  const newLead=await ok('Website consultation visible to assigned staff','academic','/leads?keyword=0987000001&branchId=1',staff);assert.equal(newLead.totalElements,1);assert.equal(newLead.content[0].source,'WEBSITE');
  const lead=query(database,"SELECT COUNT(*) FROM leads WHERE phone='0987000001' AND branch_id=1 AND interested_course_id=(SELECT id FROM courses WHERE code='MG2') AND note LIKE '%Mẫu giáo%'");assert.equal(Number(lead),1);pass('Real BFF consultation preserves course, branch and audience in lead');
  const req={requestId:crypto.randomUUID(),fullName:'Kiểm tra ngữ cảnh',phone:'0987000002',branchSlug:'cn02',programSlug:'tieng-anh-tong-quat',courseSlug:'mg2',classSlug:'cn01-l10',consent:true,note:null,website:''};
  const mismatched=await api.request('academic','/public/consultations',{body:req,headers:{'X-Website-Key':'d7-local-private-form-key-for-fixture-only'}});assert.equal(mismatched.status,400);pass('Class and branch mismatch rejected at intake');
  const future=Number(query(database,"SELECT id FROM school_classes WHERE code='CN01-L10'"));
  query(database,'UPDATE school_classes SET current_size=max_size WHERE id='+future);
  const hidden=await api.request('academic','/public/catalog/CLASS/cn01-l10');assert.equal(hidden.status,404);pass('Full class immediately removed from public catalog');query(database,'UPDATE school_classes SET current_size=0 WHERE id='+future);
  const course=Number(query(database,"SELECT id FROM courses WHERE code='IL3'"));
  const closed=await api.request('academic','/courses/'+course+'/status',{token:sa.token,method:'PATCH',body:{status:'CLOSED'}});assert.equal(closed.status,200);
  assert.equal((await api.request('academic','/public/catalog/COURSE/il3')).status,404);pass('Closed course is not publicly visible');await api.request('academic','/courses/'+course+'/status',{token:sa.token,method:'PATCH',body:{status:'OPEN'}});
  query(database,"UPDATE branches SET status='INACTIVE' WHERE id=1");
  assert.equal((await api.request('academic','/public/catalog/COURSE/gt-cn01')).status,404);assert.equal((await api.request('academic','/public/catalog/CLASS/cn01-l10')).status,404);assert.equal((await api.request('academic','/public/catalog/COURSE/mg2')).status,200);pass('Inactive branch hides its own course/class without hiding shared courses at other branches');query(database,"UPDATE branches SET status='ACTIVE' WHERE id=1");
  query(database,"UPDATE website_publications SET state='DRAFT' WHERE kind='COURSE' AND source_id=(SELECT id FROM courses WHERE code='IL4')");assert.equal((await api.request('academic','/public/catalog/COURSE/il4')).status,404);pass('Operational course is private until its CMS record is published');query(database,"UPDATE website_publications SET state='PUBLISHED' WHERE kind='COURSE' AND source_id=(SELECT id FROM courses WHERE code='IL4')");
  for(const c of relationalChecks().filter(c=>c.name!=='Website stays private until D5'))assert.equal(Number(query(database,c.sql)),c.expected,c.name);pass('63 relational/accounting invariants still pass after real HTTP workflows');
  return {status:'passed',checks,browser:browserResult,database:'isolated MySQL copy of current local data',realServices:['auth','academic','teaching','finance','api-gateway'],realModel:false,apkDevice:false,vnpayTransaction:false};
 });
 const sourceAfterHash=crypto.createHash('sha256').update(dump()).digest('hex');assert.equal(sourceAfterHash,sourceHash,'Source database changed during isolated verification');
 fs.writeFileSync(path.join(root,'docs/course-classification/',ui7?'u7-real-'+browserName+'-validation.json':'d7-validation.json'),JSON.stringify({...result,checkedAt:new Date().toISOString(),browserName,sourceUnchanged:true,sourceHash,sourceAfterHash,artifacts:path.relative(root,folder)},null,2));console.log(JSON.stringify({status:'passed',apiChecks:checks.length,browserChecks:result.browser.checks.length,sourceUnchanged:true},null,2));
}finally{if(created)query(null,'DROP DATABASE `'+database+'`');}})().catch(e=>{fs.writeFileSync(path.join(folder,'failure.json'),JSON.stringify({message:e.message,checks},null,2));console.error(e.stack);process.exitCode=1;});
