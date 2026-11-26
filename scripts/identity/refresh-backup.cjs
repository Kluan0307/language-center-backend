const {fs,path,dir,dump,query,scratch,snapshot,assert,sha,root}=require('./refresh-common.cjs');
const plan=JSON.parse(fs.readFileSync(path.join(dir,'plan.json')));assert.deepEqual(snapshot('language_center_db'),plan.baseline,'Preserved data changed since D1');
const backupDir=path.join(dir,'backup-'+Date.now());fs.mkdirSync(backupDir);const sql=dump();
assert.ok(!/^USE\s|^CREATE DATABASE/im.test(sql),'Backup must not switch databases');fs.writeFileSync(path.join(backupDir,'before.sql'),sql);
const temp=scratch('lc_refresh_restore_'+Date.now());query(null,'CREATE DATABASE `'+temp+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');
try{query(temp,sql);for(const t of plan.tables)assert.equal(Number(query(temp,'SELECT COUNT(*) FROM `'+t.name+'`')),Number(query('language_center_db','SELECT COUNT(*) FROM `'+t.name+'`')),t.name+' restore count');assert.deepEqual(snapshot(temp),plan.baseline,'Restored identity data differs');}
finally{query(null,'DROP DATABASE `'+temp+'`');}
const backup={phase:'D2',status:'complete',backup:path.relative(root,path.join(backupDir,'before.sql')),sha256:sha(sql),restoreVerified:true,tablesVerified:plan.tables.length,liveDataChanged:false};
fs.writeFileSync(path.join(dir,'backup.json'),JSON.stringify(backup,null,2));
fs.writeFileSync(path.join(root,'docs/course-classification/d2-backup-report.json'),JSON.stringify({...backup,backup:'Private ignored storage/data-refresh; see local backup.json'},null,2));console.log(JSON.stringify({...backup,backup:'private backup verified'}));
