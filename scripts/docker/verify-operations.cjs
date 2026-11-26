const c=require('./data-common.cjs'),ops=require('./operations.cjs');
const {fs,path,root,assert,docker,sha,dockerSnapshot}=c;
const checks=[],keepAlive=setInterval(()=>{},1000);let result;
const pass=name=>checks.push(name);
(async()=>{
 assert.equal(ops.doctor().status,'passed');pass('Linux engine, Compose wrapper, repo layout and archive tools valid');
 const before=dockerSnapshot(),backup=ops.createBackup();
 assert.deepEqual(backup.manifest.snapshot,before);pass('Consistent SQL/document backup preserves all tables and rows');
 assert.deepEqual(ops.readBackup(backup.manifest.id).manifest,backup.manifest);pass('Backup checksums and archive manifest validate');
 assert.throws(()=>ops.restore(backup.manifest.id,false),/ReplaceData/);pass('Main restore refuses missing explicit replace flag');
 assert.throws(()=>ops.readBackup('../other'),/Invalid backup/);pass('Backup path traversal refused');
 const privateSql=path.join(backup.dir,'database.sql'),original=fs.readFileSync(privateSql);try{fs.appendFileSync(privateSql,'\n-- altered');assert.throws(()=>ops.readBackup(backup.manifest.id),/checksum mismatch/);}finally{fs.writeFileSync(privateSql,original);}pass('Modified SQL detected before restore');
 const rehearsal=ops.rehearse(backup.manifest.id);assert.equal(rehearsal.status,'passed');pass('Real restore to scratch DB and separate volume matches all row/file fingerprints');
 assert.deepEqual(dockerSnapshot(),before);pass('Main database unchanged by backup and rehearsal');
 const stamp=Date.now(),volume='lc-dck6-restore-'+stamp+'_documents';docker(['volume','create',volume]);
 try{
  docker(['run','--rm','--network','none','--read-only','--cap-drop','ALL','--cap-add','CHOWN','--user','0:0','--mount','type=volume,source='+volume+',target=/data','--entrypoint','chown','eclipse-temurin:21-jre-jammy','10001:10001','/data']);
  const archive=docker(['run','--rm','--network','none','--read-only','--user','10001:10001','--tmpfs','/data:rw,uid=10001,gid=10001,mode=0700','--entrypoint','sh','eclipse-temurin:21-jre-jammy','-c','printf dck6-file-content > /data/probe.txt; tar -C /data -cf - .'],undefined,null);
  const manifest=ops.archiveManifest(archive);assert.equal(manifest.length,1);assert.equal(manifest[0].sha256,sha('dck6-file-content'));
  ops.replaceFiles(volume,archive);assert.deepEqual(ops.archiveManifest(ops.getArchive(volume)),manifest);pass('UID 10001 restores nonempty document archive with exact byte hashes');
  ops.replaceFiles(volume,backup.archive);assert.deepEqual(ops.archiveManifest(ops.getArchive(volume)),backup.manifest.documents);pass('Document restore removes stale files instead of merging inconsistent content');
  const unsafe=Buffer.from(archive);unsafe.write('../escape',0,'utf8');unsafe.fill(0,9,100);unsafe.fill(32,148,156);const sum=[...unsafe.subarray(0,512)].reduce((a,b)=>a+b,0);unsafe.write(sum.toString(8).padStart(6,'0')+'\0 ',148,'ascii');
  assert.throws(()=>ops.archiveManifest(unsafe),/Unsafe archive path/);pass('Archive traversal rejected before extraction');
 }finally{assert.match(volume,/^lc-dck6-restore-\d+_documents$/);docker(['volume','rm',volume]);}
 assert.deepEqual(dockerSnapshot(),before);const state=await ops.status();assert.equal(state.status,'passed');pass('All main containers healthy and application data preserved');
 const scratches=c.mysql("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name LIKE 'lc_dck6_restore_%'");assert.equal(Number(scratches),0);pass('Scratch databases cleaned after restore test');
 result={checkedAt:new Date().toISOString(),status:'passed',checks,backupId:backup.manifest.id,tables:before.tables.length,rows:before.rowCount,backupVerifiedByRealRestore:true,mainDatabaseUnchanged:true,sourceDatabaseUsed:false,temporaryVolumeRemoved:true};
 fs.writeFileSync(path.join(root,'docs/docker/dck6-operations-validation.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({status:'passed',checks:checks.length,backupId:result.backupId,tables:result.tables,rows:result.rows}));
})().catch(e=>{console.error(e.message.split('\n').slice(0,2).join('\n'));process.exitCode=1;}).finally(()=>clearInterval(keepAlive));
