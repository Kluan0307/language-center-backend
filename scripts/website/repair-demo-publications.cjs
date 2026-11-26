const {query,read,assert,literal:q,fs,path,root,dir,dump}=require('../identity/refresh-common.cjs');
const {fromDatabase,hash}=require('../identity/grade-fingerprints.cjs');
const {withServices}=require('./local-services.cjs');
async function repair(api,database,{apply=false}={}){
 const actor=await api.login('superadmin');
 const pubs=read('grade_publications',['id','exam_id','revision','draft_fingerprint','fingerprint','reason','action'],database);
 const updates=[];
 for(const p of pubs){if(p.revision!==1||p.action!=='PUBLISH'||p.reason!=='Công bố kiểm tra nội bộ')continue;
  const response=await api.request('teaching','/exams/'+p.exam_id+'/student-publication',{token:actor.token});assert.equal(response.status,200);
  const expected=fromDatabase(database,query,p.exam_id);assert.equal(expected,response.data.draftFingerprint,'Canonical seed fingerprint differs from teaching-service for '+p.exam_id);
  if(p.draft_fingerprint===expected)continue;
  assert.equal(Number(query(database,'SELECT (SELECT COUNT(*) FROM grades WHERE exam_id='+p.exam_id+' AND version<>0)+(SELECT COUNT(*) FROM exams WHERE id='+p.exam_id+' AND version<>0)')),0,'Exam/grades were edited after seeding; do not repair publication history');
  assert.equal(Number(query(database,'SELECT COUNT(*) FROM published_grades pg JOIN grades g ON g.exam_id='+p.exam_id+' AND g.student_id=pg.student_id WHERE pg.publication_id='+p.id+' AND (pg.score<>g.score OR COALESCE(pg.student_comment,\'\')<>COALESCE(g.student_comment,\'\'))')),0,'A published grade was actually changed; do not repair metadata');
  updates.push({id:p.id,examId:p.exam_id,old:p.draft_fingerprint,draft:expected,intent:hash([p.exam_id,'PUBLISH',0,expected,p.reason])});
 }
 if(apply&&updates.length){const statements=['CREATE TEMPORARY TABLE repair_assertions(ok INT CHECK(ok=1));','START TRANSACTION;'];
  for(const u of updates){statements.push('UPDATE grade_publications SET draft_fingerprint='+q(u.draft)+',fingerprint='+q(u.intent)+' WHERE id='+u.id+' AND draft_fingerprint='+q(u.old)+';','INSERT INTO repair_assertions VALUES (IF(ROW_COUNT()=1,1,0));');}
  statements.push('COMMIT;');query(database,statements.join('\n'));
 }
 return {checked:pubs.length,repaired:apply?updates.length:0,planned:updates.length,scoresChanged:false};
}
module.exports={repair};
if(require.main===module){(async()=>{const apply=process.argv.includes('--apply-local');if(apply){const folder=path.join(dir,'fingerprint-repair-'+Date.now());fs.mkdirSync(folder);fs.writeFileSync(path.join(folder,'before.sql'),dump());}
 const result=await withServices({folder:path.join(dir,'fingerprint-http-'+Date.now()),services:['auth','academic','teaching']},api=>repair(api,'language_center_db',{apply}));
 fs.writeFileSync(path.join(root,'docs/course-classification/d7-fingerprint-repair.json'),JSON.stringify({observedAt:new Date().toISOString(),...result},null,2));console.log(JSON.stringify(result));
})().catch(e=>{console.error(e.message);process.exitCode=1;});}
