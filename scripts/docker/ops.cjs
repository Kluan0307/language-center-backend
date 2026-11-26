const ops=require('./operations.cjs');
const [action,...argv]=process.argv.slice(2),args={};
for(let i=0;i<argv.length;i++){const key=argv[i];if(key==='--replace-data'){args.replaceData=true;continue;}if(!['--backup-id','--services'].includes(key)||!argv[i+1]||argv[i+1].startsWith('--'))throw Error('Invalid argument: '+key);args[key.slice(2)]=argv[++i];}
const keepAlive=setInterval(()=>{},1000);
const fs=require('fs'),path=require('path'),lock=path.join(ops.backupRoot,'.operation.lock');let ownsLock=false;
(async()=>{
 if(['backup','restore','rehearse','update','rollback'].includes(action)){
  fs.mkdirSync(ops.backupRoot,{recursive:true});
  if(fs.existsSync(lock)){const previous=JSON.parse(fs.readFileSync(lock,'utf8'));let alive=true;try{process.kill(previous.pid,0);}catch(e){if(e.code==='ESRCH')alive=false;}if(alive)throw Error('Another Docker data/image operation is running.');fs.unlinkSync(lock);}
  const fd=fs.openSync(lock,'wx');ownsLock=true;fs.writeFileSync(fd,JSON.stringify({pid:process.pid,startedAt:new Date().toISOString(),action}));fs.closeSync(fd);
 }
 let result;switch(action){
  case 'doctor':result=ops.doctor();break;
  case 'status':result=await ops.status();break;
  case 'backups':result=ops.listBackups();break;
  case 'backup':{const b=ops.createBackup();result={status:'passed',backupId:b.manifest.id,tables:b.manifest.snapshot.tables.length,rows:b.manifest.snapshot.rowCount,files:b.manifest.documents.length};break;}
  case 'rehearse':result=ops.rehearse(args['backup-id']);break;
  case 'restore':result=ops.restore(args['backup-id'],args.replaceData===true);break;
  case 'update':result=ops.update(args.services?.split(','));break;
  case 'rollback':result=ops.rollback(args['backup-id']);break;
  default:throw Error('Use doctor, status, backup, backups, rehearse, restore, update or rollback.');
 }
 console.log(JSON.stringify(result,null,2));if(result.status==='unhealthy')process.exitCode=1;
})().catch(e=>{console.error(e.message.split('\n').slice(0,2).join('\n'));process.exitCode=1;}).finally(()=>{if(ownsLock)fs.unlinkSync(lock);clearInterval(keepAlive);});
