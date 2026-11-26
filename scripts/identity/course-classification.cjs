// Local-only G2 migration, private backup, rehearsal, and review report. No credential output.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {query,dump,root}=require('./database.cjs'),{decide}=require('./course-classification-rules.cjs');
const sql=fs.readFileSync(path.join(root,'docs/migrations/V026__course_classification.sql'),'utf8');
const sha=s=>crypto.createHash('sha256').update(s).digest('hex');
const exists=(db,t)=>query(db,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='"+t+"'")==='1';
function rows(db){const website=exists(db,'website_publications');const hasColumn=query(db,"SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='program_id'")==='1';
 const sql=`SELECT JSON_OBJECT('id',c.id,'code',c.code,'name',c.name,'legacyType',c.type,'currentProgramId',${hasColumn?'c.program_id':'NULL'},'currentProgramCode',${hasColumn?'cp.code':'NULL'},'policyId',ap.id,'policyCode',ap.program_code,'policyMode',ap.mode,'websiteProgramCode',${website?'wp.code':'NULL'}) FROM courses c ${hasColumn?'LEFT JOIN programs cp ON cp.id=c.program_id':''} LEFT JOIN course_admission_policies ap ON ap.course_id=c.id AND ap.revision=(SELECT MAX(a2.revision) FROM course_admission_policies a2 WHERE a2.course_id=c.id) ${website?"LEFT JOIN website_publications pub ON pub.kind='COURSE' AND pub.source_id=c.id LEFT JOIN programs wp ON wp.id=pub.program_id":''} ORDER BY c.id`;
 const raw=query(db,sql);return raw?raw.split(/\r?\n/).map(x=>JSON.parse(x)):[];
}
function protectedState(db){const tables=['accounts','students','school_classes','enrollments','tuition','payments'];return {counts:Object.fromEntries(tables.filter(t=>exists(db,t)).map(t=>[t,Number(query(db,'SELECT COUNT(*) FROM `'+t+'`'))])),policies:sha(query(db,'SELECT * FROM course_admission_policies ORDER BY id')),legacy:sha(query(db,'SELECT id,type FROM courses ORDER BY id'))};}
function migrate(db){const before=protectedState(db);query(db,sql);const report=rows(db).map(row=>({...row,...decide(row),audienceStatus:'PENDING_CONFIRMATION'}));
 for(const r of report){if(r.status==='AUTO'){
  assert.ok(Number.isSafeInteger(r.id)&&r.id>0);assert.ok(['GE','COMMUNICATION','IELTS','TOEIC'].includes(r.suggested));
  query(db,`UPDATE courses c JOIN programs p ON p.code='${r.suggested}' SET c.program_id=p.id WHERE c.id=${r.id} AND c.program_id IS NULL`);
 }}
 assert.deepEqual(protectedState(db),before,'Protected records changed');
 assert.equal(query(db,'SELECT COUNT(*) FROM audience_groups WHERE code IN (\'PRESCHOOL\',\'PRIMARY\',\'SECONDARY\',\'HIGH_SCHOOL\',\'ADULT\')'),'5');
 const assigned=rowsCached(db);
 return report.map(r=>({...r,assignedProgramId:assigned.get(r.id),audienceStatus:'PENDING_CONFIRMATION'}));
}
function rowsCached(db){return new Map(rows(db).map(r=>[r.id,r.currentProgramId]));}
const apply=process.argv.includes('--apply'),target='language_center_db',stamp=Date.now(),temporary='lc_course_g2_'+stamp;
const privateFolder=path.join(root,'storage/course-classification',String(stamp));fs.mkdirSync(privateFolder,{recursive:true});
const snapshot=dump();fs.writeFileSync(path.join(privateFolder,'before.sql'),snapshot);let report;
query(null,'CREATE DATABASE `'+temporary+'` CHARACTER SET utf8mb4;');
try{
 query(temporary,snapshot);const first=migrate(temporary);migrate(temporary);assert.equal(rows(temporary).filter(r=>r.currentProgramId).length,first.filter(r=>r.assignedProgramId).length);
 // Integrity: invalid program FK must be rejected.
 if(first.length){assert.throws(()=>query(temporary,'UPDATE courses SET program_id=9223372036854775807 WHERE id='+first[0].id));}
 report=apply?migrate(target):first;
 const result={date:'2026-11-16',applied:apply,rehearsal:'passed',replay:'passed',foreignKey:'passed',courses:report.length,assigned:report.filter(r=>r.assignedProgramId).length,review:report.filter(r=>!r.assignedProgramId).length,audiencesPending:report.length,protectedData:'unchanged',backup:'private storage/course-classification/'+stamp+'/before.sql',rows:report};
 fs.writeFileSync(path.join(privateFolder,'report.json'),JSON.stringify(result,null,2));
 fs.mkdirSync(path.join(root,'docs/course-classification'),{recursive:true});fs.writeFileSync(path.join(root,'docs/course-classification/g2-report.json'),JSON.stringify(result,null,2));
 console.log(JSON.stringify({...result,rows:undefined},null,2));
}finally{query(null,'DROP DATABASE `'+temporary+'`;');}
