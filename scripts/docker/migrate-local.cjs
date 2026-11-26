// DCK5: copy a consistent local snapshot to an EMPTY, separate Docker DB.
// SQL and document backups contain private data and stay under ignored storage/.
const c=require('./data-common.cjs'),{fs,path,assert,root,database,compose,mysql,sha,snapshot,dockerSnapshot,context,volumeArchive,restoreFiles,files,execFileSync}=c;
const source=require('../identity/database.cjs');
const sourceQuery=(db,sql)=>source.query(db,"SET time_zone='+07:00';\n"+sql);
const run=String(Date.now()),dir=path.join(root,'storage/docker-backups/dck5-'+run),scratch='lc_dck5_restore_'+run;
const keepAlive=setInterval(()=>{},1000);let scratchCreated=false,stopped=false,changed=false,complete=false;
const apps=['web-admin','website','assistant-service','api-gateway','auth-service','academic-service','teaching-service','finance-service'];
async function main(){
 const ctx=context(),before=dockerSnapshot();
 assert.ok([87,88].includes(before.tables.length));assert.equal(before.rowCount,0,'Docker DB already has rows. Refusing to overwrite; use a reviewed backup/restore procedure.');
 const sourceBefore=snapshot(sourceQuery);assert.ok([87,88].includes(sourceBefore.tables.length));assert.ok(sourceBefore.rowCount>0,'Source is empty');
 const other=Number(sourceQuery(database,"SELECT (SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.events WHERE event_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type<>'BASE TABLE')"));assert.equal(other,0,'Review triggers/routines/events/views before migration');
 assert.equal(Number(sourceQuery(database,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND engine<>'InnoDB'")),0,'Single-transaction backup requires InnoDB tables');
 fs.mkdirSync(dir,{recursive:true});
 const sql=source.dump();assert.ok(!/^USE\s|^CREATE DATABASE/im.test(sql),'Dump must not switch databases');
 fs.writeFileSync(path.join(dir,'source.sql'),sql,{mode:0o600});
 assert.equal(snapshot(sourceQuery).sha256,sourceBefore.sha256,'Source changed while backing up. Retry during an idle period.');
 const env=fs.readFileSync(path.join(root,'.env.local'),'utf8'),configured=env.match(/^DOCUMENT_STORAGE=(.*)$/m)?.[1].trim().replace(/^(['"])(.*)\1$/,'$2');
 const candidates=configured?[path.resolve(root,configured)]:[path.join(root,'storage/documents'),path.join(root,'teaching-service/storage/documents')];
 const populated=candidates.filter(p=>files(p).length);assert.ok(populated.length<=1,'Multiple document directories have files; set DOCUMENT_STORAGE explicitly.');
 const sourceDocuments=populated[0]||candidates.find(p=>fs.existsSync(p))||candidates[0],manifest=files(sourceDocuments);
 const keys=sourceQuery(database,'SELECT storage_key FROM documents UNION SELECT storage_key FROM document_events WHERE storage_key IS NOT NULL').split(/\r?\n/).filter(Boolean);
 for(const key of keys){assert.match(key,/^[0-9a-f-]{36}$/);assert.ok(manifest.some(f=>f.name===key),'Missing document referenced by source database');}
 const docBackup=path.join(dir,'documents');fs.mkdirSync(docBackup);
 if(fs.existsSync(sourceDocuments))fs.cpSync(sourceDocuments,docBackup,{recursive:true,dereference:false});
 assert.deepEqual(files(docBackup),manifest,'Source documents changed during backup');
 execFileSync('tar.exe',['-C',docBackup,'-cf',path.join(dir,'documents.tar'),'.'],{windowsHide:true,stdio:['ignore','pipe','pipe']});
 const rollback=compose(['exec','-T','mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump --user=root --single-transaction --no-tablespaces --skip-comments --set-gtid-purged=OFF --default-character-set=utf8mb4 "$MYSQL_DATABASE"']);
 fs.writeFileSync(path.join(dir,'docker-before.sql'),rollback,{mode:0o600});
 fs.writeFileSync(path.join(dir,'docker-documents-before.tar'),volumeArchive(ctx.documentsVolume));
 fs.writeFileSync(path.join(dir,'manifest.json'),JSON.stringify({createdAt:new Date().toISOString(),source:sourceBefore,targetBefore:before,sourceDocuments,documents:manifest,sourceSqlSha256:sha(sql),targetSqlSha256:sha(rollback)},null,2));
 console.log('Private DB/file backups saved; rehearsing restore into a separate Docker scratch database.');
 mysql('CREATE DATABASE `'+scratch+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');scratchCreated=true;mysql(sql,scratch);
 const rehearsal=snapshot((db,q)=>mysql(q,db),scratch);assert.deepEqual(rehearsal,sourceBefore,'Scratch restore differs from source');
 mysql('DROP DATABASE `'+scratch+'`');scratchCreated=false;
 assert.equal(snapshot(sourceQuery).sha256,sourceBefore.sha256,'Source changed during rehearsal. Target was not touched.');
 compose(['stop',...apps]);stopped=true;
 assert.equal(dockerSnapshot().sha256,before.sha256,'Docker DB changed before import');
 changed=true;mysql(sql);restoreFiles(ctx.documentsVolume,fs.readFileSync(path.join(dir,'documents.tar')));
 mysql(fs.readFileSync(path.join(root,'docs/migrations/V029__cloudinary_assets.sql'),'utf8'));
 const restored=dockerSnapshot();
 assert.deepEqual(restored.tables.filter(t=>sourceBefore.tables.some(s=>s.name===t.name)),sourceBefore.tables,'Target restore differs from source');
 assert.ok(restored.tables.filter(t=>!sourceBefore.tables.some(s=>s.name===t.name)).every(t=>t.name==='media_assets'&&t.rows===0),'Unexpected additional target data');
 assert.equal(snapshot(sourceQuery).sha256,sourceBefore.sha256,'Source changed during migration');
 assert.deepEqual(files(sourceDocuments),manifest,'Source documents changed during migration');
 compose(['up','-d','--no-build','--wait','--wait-timeout','240']);stopped=false;
 const final=context();assert.equal(final.states.length,9);assert.ok(final.states.every(s=>s.State.Health?.Status==='healthy'));
 const result={checkedAt:new Date().toISOString(),status:'passed',tableCount:restored.tables.length,rowsImported:sourceBefore.rowCount,tables:sourceBefore.tables.map(({name,rows,sha256})=>({name,rows,sha256})),sourceSha256:sourceBefore.sha256,restoredSha256:rehearsal.sha256,rehearsalVerified:true,targetVerified:true,sourceUnchanged:true,sourceDocumentsUnchanged:true,documentFiles:manifest.length,documentReferences:keys.length,backupFolder:path.relative(root,dir).replaceAll('\\','/'),backupSha256:sha(sql),services:final.states.map(s=>({service:s.Config.Labels['com.docker.compose.service'],health:s.State.Health.Status})),externalProvidersEnabled:false};
 fs.writeFileSync(path.join(dir,'completed.json'),JSON.stringify(result,null,2));fs.writeFileSync(path.join(root,'docs/docker/dck5-migration.json'),JSON.stringify({...result,backupFolder:'Private ignored storage/docker-backups; see local completed.json'},null,2));complete=true;
 console.log(JSON.stringify({status:'passed',tables:restored.tables.length,rowsImported:result.rowsImported,documentFiles:manifest.length,sourceUnchanged:true,healthyContainers:9}));
}
main().catch(e=>{console.error('DCK5 migration stopped: '+e.message);if(changed&&!complete)console.error('Target was modified. Apps remain stopped if restore validation failed; recover using docker-before.sql and docker-documents-before.tar in the private backup folder.');process.exitCode=1;}).finally(()=>{if(scratchCreated)try{mysql('DROP DATABASE `'+scratch+'`');}catch{console.error('Scratch cleanup failed: '+scratch);}if(stopped&&!changed)try{compose(['up','-d','--no-build','--wait','--wait-timeout','240']);}catch{console.error('Restart the Docker stack with compose.ps1.');}clearInterval(keepAlive);});
