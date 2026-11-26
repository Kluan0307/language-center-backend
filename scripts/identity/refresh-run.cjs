const {fs,path,dir,root,query,dump,sha,scratch,snapshot,keep,assert}=require('./refresh-common.cjs');
const {checks,sqlAssertions}=require('./refresh-validation.cjs');
const plan=JSON.parse(fs.readFileSync(path.join(dir,'plan.json'))),manifest=JSON.parse(fs.readFileSync(path.join(dir,'manifest.json'))),backup=JSON.parse(fs.readFileSync(path.join(dir,'backup.json')));
const seed=fs.readFileSync(path.join(dir,'seed-body.sql'),'utf8')+'\n'+sqlAssertions()+'\nCOMMIT;\n';
fs.writeFileSync(path.join(dir,'reset-and-seed.sql'),seed);
function verify(target){const report=[];
 for(const c of checks()){assert.equal(Number(query(target,c.sql)),c.expected,c.name);report.push(c.name);}
 for(const [t,n] of Object.entries(manifest.counts).filter(([t])=>!['program_levels','student_code_numbers'].includes(t)))assert.equal(Number(query(target,'SELECT COUNT(*) FROM `'+t+'`')),n,t+' count');
 const after=snapshot(target);for(const t of keep)assert.equal(after[t],plan.baseline[t],'Preserved table changed: '+t);
 for(const a of plan.accounts)assert.equal(Number(query(target,'SELECT session_version FROM accounts WHERE id='+a.id)),Number(a.session_version??0)+1,'Session not invalidated '+a.id);
 assert.equal(Number(query(target,"SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE'")),plan.tables.length);
 const inv=JSON.parse(fs.readFileSync(path.join(dir,'inventory.json')));
 const columns=JSON.parse(query(target,"SELECT JSON_ARRAYAGG(JSON_OBJECT('table',TABLE_NAME,'column',COLUMN_NAME,'type',COLUMN_TYPE,'nullable',IS_NULLABLE,'default',COLUMN_DEFAULT,'extra',EXTRA)) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()"));
 const signature=xs=>xs.map(c=>JSON.stringify(c,Object.keys(c).sort())).sort();
 assert.deepEqual(signature(columns),signature(inv.columns),'Database columns changed');
 const counts=Object.fromEntries(['accounts','branches','programs','program_levels','audience_groups','courses','school_classes','class_sessions','students','enrollments','tuition','payments','leads','grades','published_grades','student_notifications','website_publications'].map(t=>[t,Number(query(target,'SELECT COUNT(*) FROM `'+t+'`'))]));
 return {checks:report.length,checkNames:report,counts,identityPreserved:true,allStudentAccountsRelinked:true,allSessionsInvalidated:true};
}
if(!process.argv.includes('--apply-local')){
 const target=scratch('lc_refresh_rehearsal_'+Date.now()),before=fs.readFileSync(path.join(root,backup.backup),'utf8');assert.equal(sha(before),backup.sha256);
 query(null,'CREATE DATABASE `'+target+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');
 try{query(target,before);query(target,seed);const first=verify(target);
 // Prove assertion failure rolls back DML, including the initial DELETEs.
 const probe=scratch('lc_refresh_probe_'+Date.now());query(null,'CREATE DATABASE `'+probe+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');
 try{query(probe,before);assert.throws(()=>query(probe,seed.replace('COMMIT;','INSERT INTO refresh_assertions VALUES (\'rollback probe\',0);\nCOMMIT;')));
 for(const t of plan.tables)assert.equal(Number(query(probe,'SELECT COUNT(*) FROM `'+t.name+'`')),t.count,'Rollback failed '+t.name);}
 finally{query(null,'DROP DATABASE `'+probe+'`');}
 const result={phase:'D3',status:'rehearsal-passed',seedSha256:sha(seed),rollbackProbePassed:true,...first};
 fs.writeFileSync(path.join(dir,'rehearsal.json'),JSON.stringify(result,null,2));fs.writeFileSync(path.join(root,'docs/course-classification/d3-rehearsal-report.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({...result,checkNames:undefined}));
 }finally{query(null,'DROP DATABASE `'+target+'`');}
}else{
 const rehearsal=JSON.parse(fs.readFileSync(path.join(dir,'rehearsal.json')));assert.equal(rehearsal.seedSha256,sha(seed),'Seed changed after rehearsal');assert.ok(rehearsal.rollbackProbePassed);
 assert.equal(sha(dump()),backup.sha256,'Source database changed after backup: create a new plan/backup first');
 assert.equal(Number(query('language_center_db',"SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE' AND ENGINE<>'InnoDB'")),0,'Transactional InnoDB tables required');
 const fresh=path.join(dir,'before-apply-'+Date.now()+'.sql');fs.writeFileSync(fresh,dump());
 assert.equal(sha(fs.readFileSync(fresh,'utf8')),backup.sha256);
 // The source host/schema restriction is enforced by database.cjs. No schema DDL is sent here.
 query('language_center_db',seed);
 const result={phase:'D4',status:'applied-local',appliedAt:new Date().toISOString(),seedSha256:sha(seed),...verify('language_center_db'),backup:'private storage/data-refresh',schemaPreserved:true};
 fs.writeFileSync(path.join(dir,'applied.json'),JSON.stringify(result,null,2));fs.writeFileSync(path.join(root,'docs/course-classification/d4-apply-report.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({...result,checkNames:undefined}));
}
