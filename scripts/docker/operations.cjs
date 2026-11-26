// Local operational tooling. Main DB changes require an explicit restore action.
const c=require('./data-common.cjs');
const {fs,path,assert,root,workspace,database,docker,compose,mysql,sha,snapshot,dockerSnapshot,context,volumeArchive,identifier}=c;
const backupRoot=path.join(root,'storage/docker-backups'),helper='eclipse-temurin:21-jre-jammy';
const appNames=['web-admin','website','assistant-service','api-gateway','auth-service','academic-service','teaching-service','finance-service'];
function id(){return 'ops-'+Date.now()+'-'+require('node:crypto').randomBytes(3).toString('hex');}
function backupPath(name){assert.match(name,/^ops-\d+-[a-f0-9]{6}$/,'Invalid backup ID');const p=path.join(backupRoot,name);assert.ok(p.startsWith(backupRoot+path.sep));return p;}
function service(s){return s.Config.Labels['com.docker.compose.service'];}
function resume(names){if(names.length)compose(['up','-d','--no-deps','--no-build','--wait','--wait-timeout','240',...names]);}
function pause(names){if(names.length)compose(['stop',...names]);}
function exportSql(){return compose(['exec','-T','mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump --user=root --single-transaction --no-tablespaces --skip-comments --set-gtid-purged=OFF --hex-blob --default-character-set=utf8mb4 "$MYSQL_DATABASE"']);}
function supportedSchema(db=database){assert.equal(Number(mysql("SELECT (SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.events WHERE event_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND (table_type<>'BASE TABLE' OR engine<>'InnoDB'))",db)),0,'Review non-InnoDB tables, views, routines, triggers or events before using this tool.');}
function archiveManifest(archive){
 const result=[];let offset=0;
 while(offset+512<=archive.length){const h=archive.subarray(offset,offset+512);if(h.every(b=>b===0))break;
  const string=(start,length)=>h.subarray(start,start+length).toString('utf8').split('\0')[0];
  const sum=Number.parseInt(string(148,8).trim(),8);assert.equal([...h].reduce((n,b,i)=>n+(i>=148&&i<156?32:b),0),sum,'Invalid tar header');
  const prefix=string(345,155),name=(prefix?prefix+'/':'')+string(0,100),type=string(156,1),size=Number.parseInt(string(124,12).trim()||'0',8);
  assert.ok(Number.isSafeInteger(size)&&size>=0&&offset+512+size<=archive.length,'Invalid tar size');
  assert.ok(!name.startsWith('/')&&!name.includes('\\')&&!name.includes(':')&&!name.split('/').includes('..'),'Unsafe archive path');
  assert.ok(['','0','5'].includes(type),'Links, special files and extended tar headers are not supported');
  if(type!=='5'){const clean=name.replace(/^\.\//,'');assert.ok(clean&&clean!=='.');assert.ok(!result.some(f=>f.name===clean),'Duplicate archive path');result.push({name:clean,bytes:size,sha256:sha(archive.subarray(offset+512,offset+512+size))});}
  offset+=512+Math.ceil(size/512)*512;
 }
 return result.sort((a,b)=>a.name.localeCompare(b.name));
}
function volumeAllowed(name){assert.ok(name==='language-center_documents'||/^lc-dck6-restore-\d+_documents$/.test(name),'Unexpected document volume');}
function getArchive(volume){volumeAllowed(volume);if(volume==='language-center_documents')return volumeArchive(volume);return docker(['run','--rm','--network','none','--read-only','--cap-drop','ALL','--user','10001:10001','--mount','type=volume,source='+volume+',target=/data,readonly','--entrypoint','tar',helper,'-C','/data','-cf','-','.'],undefined,null);}
function replaceFiles(volume,archive){
 volumeAllowed(volume);archiveManifest(archive);
 // /data is the validated named volume, never a bind mount or a computed host path.
 docker(['run','--rm','-i','--network','none','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges:true','--user','10001:10001','--mount','type=volume,source='+volume+',target=/data','--entrypoint','sh',helper,'-c','find /data -mindepth 1 -maxdepth 1 -exec rm -rf -- {} + && exec tar -C /data -xf -'],archive,null);
}
function loadSql(sql,db=database){
 assert.ok(!/^\s*(USE\s|CREATE\s+DATABASE|DROP\s+DATABASE)/im.test(sql),'Backup cannot switch or drop databases');
 supportedSchema(db);
 const names=mysql("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'",db).split(/\r?\n/).filter(Boolean);
 const drop=names.length?'SET FOREIGN_KEY_CHECKS=0; DROP TABLE '+names.map(identifier).join(',')+'; SET FOREIGN_KEY_CHECKS=1;\n':'';
 mysql(drop+sql,db);
}
function readBackup(name){
 const dir=backupPath(name);assert.ok(!fs.lstatSync(dir).isSymbolicLink(),'Backup directory cannot be a link');
 for(const file of ['backup.json','database.sql','documents.tar'])assert.ok(!fs.lstatSync(path.join(dir,file)).isSymbolicLink(),'Backup file cannot be a link');
 const manifest=JSON.parse(fs.readFileSync(path.join(dir,'backup.json'),'utf8'));assert.equal(manifest.version,1);assert.equal(manifest.id,name);assert.equal(manifest.database,database);assert.equal(manifest.project,'language-center');
 const sql=fs.readFileSync(path.join(dir,'database.sql'),'utf8'),archive=fs.readFileSync(path.join(dir,'documents.tar'));
 assert.equal(sha(sql),manifest.databaseSqlSha256,'SQL backup checksum mismatch');assert.equal(sha(archive),manifest.documentsTarSha256,'Document archive checksum mismatch');
 assert.deepEqual(archiveManifest(archive),manifest.documents,'Document file manifest mismatch');
 return {dir,manifest,sql,archive};
}
function createBackup({leaveStopped=false}={}){
 const ctx=context(),running=ctx.states.filter(s=>service(s)!=='mysql'&&s.State.Running).map(service);
 const name=id(),dir=backupPath(name);fs.mkdirSync(dir,{recursive:true});let done=false;
 try{
  pause(running);
  supportedSchema();const before=dockerSnapshot(),sql=exportSql(),archive=getArchive(ctx.documentsVolume),documents=archiveManifest(archive);
  assert.equal(dockerSnapshot().sha256,before.sha256,'DB changed during backup; stop external writers and retry');
  assert.deepEqual(archiveManifest(getArchive(ctx.documentsVolume)),documents,'Documents changed during backup');
  const manifest={version:1,id:name,createdAt:new Date().toISOString(),project:'language-center',database,snapshot:before,databaseSqlSha256:sha(sql),documentsTarSha256:sha(archive),documents,runningServices:running,images:ctx.states.map(s=>({service:service(s),reference:s.Config.Image,id:s.Image})),externalSecretsIncluded:false};
  fs.writeFileSync(path.join(dir,'database.sql'),sql,{mode:0o600});fs.writeFileSync(path.join(dir,'documents.tar'),archive,{mode:0o600});fs.writeFileSync(path.join(dir,'backup.json'),JSON.stringify(manifest,null,2),{mode:0o600});
  done=true;console.log('Backup complete: '+name+' ('+before.tables.length+' tables, '+before.rowCount+' rows, '+documents.length+' files)');return {dir,manifest,sql,archive,running};
 }finally{if(!leaveStopped||!done)resume(running);}
}
function rehearse(name){
 const backup=readBackup(name),stamp=Date.now(),db='lc_dck6_restore_'+stamp,volume='lc-dck6-restore-'+stamp+'_documents';
 let createdDb=false,createdVolume=false;
 try{
  mysql('CREATE DATABASE '+identifier(db)+' CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');createdDb=true;
  mysql('CREATE TABLE dck6_stale_probe(id INT PRIMARY KEY); INSERT INTO dck6_stale_probe VALUES(1)',db);
  loadSql(backup.sql,db);assert.deepEqual(snapshot((database,sql)=>mysql(sql,database),db),backup.manifest.snapshot,'Restored database differs from backup');
  docker(['volume','create',volume]);createdVolume=true;
  docker(['run','--rm','--network','none','--read-only','--cap-drop','ALL','--cap-add','CHOWN','--user','0:0','--mount','type=volume,source='+volume+',target=/data','--entrypoint','chown',helper,'10001:10001','/data']);
  replaceFiles(volume,backup.archive);assert.deepEqual(archiveManifest(getArchive(volume)),backup.manifest.documents,'Restored documents differ from backup');
  const result={checkedAt:new Date().toISOString(),status:'passed',backupId:name,tables:backup.manifest.snapshot.tables.length,rows:backup.manifest.snapshot.rowCount,documentFiles:backup.manifest.documents.length,mainDatabaseWritten:false,mainVolumeWritten:false};
  fs.writeFileSync(path.join(backup.dir,'rehearsal.json'),JSON.stringify(result,null,2));return result;
 }finally{if(createdDb){assert.match(db,/^lc_dck6_restore_\d+$/);mysql('DROP DATABASE '+identifier(db));}if(createdVolume){assert.match(volume,/^lc-dck6-restore-\d+_documents$/);docker(['volume','rm',volume]);}}
}
function restore(name,replaceData){
 assert.equal(replaceData,true,'Restore replaces Docker data. Pass -ReplaceData with an explicit backup ID.');
 const backup=readBackup(name);rehearse(name);const safety=createBackup({leaveStopped:true});let restored=false;
 try{
  loadSql(backup.sql);replaceFiles('language-center_documents',backup.archive);
  assert.deepEqual(dockerSnapshot(),backup.manifest.snapshot,'Restored DB fingerprint mismatch');assert.deepEqual(archiveManifest(getArchive('language-center_documents')),backup.manifest.documents);
  restored=true;resume(safety.running);
  const result={checkedAt:new Date().toISOString(),status:'passed',backupId:name,safetyBackupId:safety.manifest.id};fs.writeFileSync(path.join(safety.dir,'restore-result.json'),JSON.stringify(result,null,2));return result;
 }catch(error){
  if(!restored)try{loadSql(safety.sql);replaceFiles('language-center_documents',safety.archive);assert.deepEqual(dockerSnapshot(),safety.manifest.snapshot);assert.deepEqual(archiveManifest(getArchive('language-center_documents')),safety.manifest.documents);resume(safety.running);console.error('Previous state recovered from safety backup '+safety.manifest.id);}catch{throw Error('Restore/rollback failed. Apps remain stopped. Recover with safety backup '+safety.manifest.id);}
  throw error;
 }
}
function doctor(){
 assert.equal(docker(['info','--format','{{.OSType}}']).trim(),'linux','Docker Desktop must use Linux containers');
 assert.ok(fs.existsSync(path.join(workspace,'.env.docker.local')),'Run init-env.cjs first');
 for(const repo of ['language-center-web-frontend','language-center-website'])assert.ok(fs.existsSync(path.join(workspace,repo,'Dockerfile')),'Missing sibling repo '+repo);
 compose(['config','--quiet']);docker(['run','--rm','--network','none','--entrypoint','tar',helper,'--version']);return {status:'passed',engine:'linux',configuration:'valid',backupTools:'ready'};
}
async function status(){
 const ctx=context(),config=JSON.parse(compose(['config','--format','json']));
 const urls=Object.fromEntries([['web-admin',8080],['website',3000],['api-gateway',8080]].map(([s,p])=>[s,'http://'+compose(['port',s,String(p)]).split(/\r?\n/)[0]]));
 const response=await fetch(urls['api-gateway']+'/actuator/health/readiness',{signal:AbortSignal.timeout(10000)});assert.equal(response.status,200,'Gateway not ready');
 const result={checkedAt:new Date().toISOString(),status:ctx.states.every(s=>s.State.Running&&s.State.Health?.Status==='healthy')?'passed':'unhealthy',urls,services:ctx.states.map(s=>({service:service(s),state:s.State.Status,health:s.State.Health?.Status})),data:Object.fromEntries(['accounts','students','courses','school_classes'].map(t=>[t,Number(mysql('SELECT COUNT(*) FROM '+identifier(t)))])),providers:{assistant:config.services['assistant-service'].environment.ASSISTANT_CHAT_ENABLED==='true',vnpay:config.services['finance-service'].environment.VNPAY_ENABLED==='true',cloudinary:config.services['academic-service'].environment.CLOUDINARY_ENABLED==='true'&&!!config.services['academic-service'].environment.CLOUDINARY_API_SECRET}};
 fs.writeFileSync(path.join(root,'docs/docker/runtime-status.json'),JSON.stringify(result,null,2));return result;
}
function refreshConsumers(names,states){
 const manifest=JSON.parse(fs.readFileSync(path.join(root,'deployment/docker/services.json'),'utf8'));
 const consumers=manifest.filter(s=>s.calls.some(target=>names.includes(target))).map(s=>s.name);
 if(names.some(n=>manifest.some(s=>s.name===n)))consumers.push('website');
 const selected=[...new Set(consumers)].filter(n=>!names.includes(n)&&states.some(s=>service(s)===n&&s.State.Running));
 if(selected.length){console.log('Refreshing callers: '+selected.join(', '));compose(['restart',...selected]);resume(selected);}
 return selected;
}
function update(names){
 const ctx=context();names=names?.length?names:ctx.states.filter(s=>s.State.Running&&service(s)!=='mysql').map(service);
 assert.ok(names.length>0&&new Set(names).size===names.length&&names.every(n=>appNames.includes(n)),'Select application services only; MySQL upgrades are separate.');
 const backup=createBackup(),config=JSON.parse(compose(['config','--format','json']));
 const previous=ctx.states.filter(s=>names.includes(service(s))).map(s=>({service:service(s),reference:s.Config.Image,id:s.Image}));assert.equal(previous.length,names.length);
 const running=previous.filter(p=>ctx.states.find(s=>service(s)===p.service).State.Running).map(p=>p.service);
 for(const old of previous)docker(['tag',old.id,'language-center/'+old.service+':rollback-'+backup.manifest.id]);
 fs.writeFileSync(path.join(backup.dir,'release.json'),JSON.stringify({previous,services:names},null,2));
 try{
  for(const name of names){const b=config.services[name].build;assert.ok(b,'No build definition for '+name);const args=['build','--file',path.join(b.context,b.dockerfile||'Dockerfile'),'--tag',config.services[name].image];for(const [key,value] of Object.entries(b.args||{}))args.push('--build-arg',key+'='+value);args.push(b.context);console.log('Building '+name);docker(args);}
  if(running.length)compose(['up','-d','--no-deps','--no-build','--force-recreate','--wait','--wait-timeout','240',...running]);
  const refreshedConsumers=refreshConsumers(names,ctx.states);
  return {status:'passed',updated:names,refreshedConsumers,backupId:backup.manifest.id,rollbackImagesRetained:true};
 }catch(error){for(const old of previous)docker(['tag',old.id,old.reference]);resume(running);refreshConsumers(names,ctx.states);throw Error('Update failed; restored prior image tags. Backup: '+backup.manifest.id+'. '+error.message.split('\n')[0]);}
}
function rollback(name){const dir=backupPath(name),release=JSON.parse(fs.readFileSync(path.join(dir,'release.json'),'utf8'));readBackup(name);const running=context().states.filter(s=>s.State.Running&&release.services.includes(service(s))).map(service);for(const old of release.previous){assert.ok(appNames.includes(old.service));assert.match(old.id,/^sha256:[a-f0-9]{64}$/);assert.match(old.reference,/^language-center\/[a-z-]+:[A-Za-z0-9_.-]+$/);docker(['tag',old.id,old.reference]);}if(running.length)compose(['up','-d','--no-deps','--no-build','--force-recreate','--wait','--wait-timeout','240',...running]);refreshConsumers(release.services,context().states);return {status:'passed',imageRollback:name,databaseRestored:false};}
function listBackups(){return {status:'passed',backups:fs.existsSync(backupRoot)?fs.readdirSync(backupRoot).filter(n=>/^ops-\d+-[a-f0-9]{6}$/.test(n)).sort().reverse().flatMap(name=>{const file=path.join(backupPath(name),'backup.json');if(!fs.existsSync(file))return [];const m=JSON.parse(fs.readFileSync(file,'utf8'));return [{id:name,createdAt:m.createdAt,tables:m.snapshot.tables.length,rows:m.snapshot.rowCount,files:m.documents.length,rehearsed:fs.existsSync(path.join(backupPath(name),'rehearsal.json')),hasImageRollback:fs.existsSync(path.join(backupPath(name),'release.json'))}];}):[]};}
module.exports={backupRoot,backupPath,archiveManifest,readBackup,createBackup,rehearse,restore,doctor,status,update,rollback,listBackups,getArchive,replaceFiles,loadSql,refreshConsumers};
