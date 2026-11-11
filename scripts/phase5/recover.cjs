const fs=require('fs'),path=require('path'),Module=require('module'),crypto=require('crypto'),{execFileSync}=require('child_process');
const root=path.resolve(__dirname,'../..');process.chdir(root);const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));const sourceDir=path.resolve(process.argv[2]||fs.readFileSync('storage/phase5-latest.txt','utf8').trim()),source=read(path.join(sourceDir,'runtime.json'));
if(!sourceDir.startsWith(path.join(root,'storage/acceptance')+path.sep)||!/^lc_rehearsal_\w+$/.test(source.database))throw Error('Private source required');
if(read(path.join(sourceDir,'acceptance-summary.json')).status!=='PASSED'||read(path.join(sourceDir,'phase5-live-ui.json')).status!=='PASSED')throw Error('Acceptance evidence missing');
const saved={};for(const line of fs.readFileSync('.env.local','utf8').split(/\r?\n/)){const m=line.match(/^(DB_URL|DB_USERNAME|DB_PASSWORD)=(.*)$/);if(m)saved[m[1]]=m[2];}
const env={...saved,DB_URL:saved.DB_URL.replace(/^(jdbc:mysql:\/\/[^/]+\/)[^?]+/,'$1'+source.database)};
const privateEnv=path.join(sourceDir,'recovery.private.env');fs.writeFileSync(privateEnv,Object.entries(env).map(([k,v])=>k+'='+v).join('\n'));
const backup=path.join(sourceDir,'recovery-backup-'+crypto.randomBytes(3).toString('hex'));const docs=read(path.join(source.backup,'rehearsal.json')).restoredDocuments;
const fd=fs.openSync(path.join(sourceDir,'recovery.console.log'),'a');
function run(args){execFileSync(process.execPath,['scripts/operations.cjs',...args],{cwd:root,env:{...process.env,...env},windowsHide:true,stdio:['ignore',fd,fd]});}
async function main(){
 const needle=source.releaseRoot ? '-Dlc.rehearsal='+source.runId : sourceDir.replaceAll('\\','/').replaceAll("'","''")+'/jars/';
 execFileSync('powershell.exe',['-NoProfile','-Command',`$p=Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {$_.CommandLine -and $_.CommandLine.Replace('\\','/').Contains('${needle}')};if($p){throw 'Private writers must be stopped before backup'}`],{windowsHide:true,stdio:['ignore',fd,fd]});
 run(['backup','--env',privateEnv,'--writes-paused','--output',backup,'--documents',docs]);run(['verify','--backup',backup]);run(['rehearse','--env',privateEnv,'--backup',backup]);
 const restored=read(path.join(backup,'rehearsal.json')),manifest=read(path.join(backup,'manifest.json'));
 if(restored.database===source.database||!/^lc_rehearsal_\w+$/.test(restored.database))throw Error('Restore target unsafe');
 const filename=path.join(root,'scripts/operations.cjs'),script=fs.readFileSync(filename,'utf8'),mod=new Module(filename);mod.filename=filename;mod.paths=module.paths;mod._compile(script.slice(0,script.indexOf('module.exports ='))+'\nmodule.exports={dbConfig,tableCounts,dataFingerprint};',filename);Object.assign(process.env,env);const h=mod.exports,db=h.dbConfig();
 const dataSha256=await h.dataFingerprint(db,restored.database),counts=await h.tableCounts(db,restored.database);
 const passed=dataSha256===manifest.dataSha256&&JSON.stringify(counts)===JSON.stringify(manifest.rowCounts);
 const dir=sourceDir+'-restore';fs.mkdirSync(dir,{recursive:false});
 const runtime={...source,runId:source.runId+'-restore',dir,database:restored.database,backup,parentRunId:source.runId,createdAt:new Date().toISOString()};delete runtime.vitePid;delete runtime.browserLauncherPid;
 fs.writeFileSync(path.join(dir,'runtime.json'),JSON.stringify(runtime,null,2));for(const file of ['expected.json','fixture.json','private-session.json'])fs.copyFileSync(path.join(sourceDir,file),path.join(dir,file));
 const proof={status:passed?'PASSED':'FAILED',sourceDatabase:source.database,restoredDatabase:restored.database,backup,consistency:manifest.consistency,expectedDataSha256:manifest.dataSha256,restoredDataSha256:dataSha256,rowCountsMatch:JSON.stringify(counts)===JSON.stringify(manifest.rowCounts),tables:Object.keys(counts).length,referencedFiles:restored.referencedFiles,migrations:restored.migrations,restoredRunDir:dir};
 fs.writeFileSync(path.join(sourceDir,source.releaseRoot?'release-recovery.json':'phase5-recovery.json'),JSON.stringify(proof,null,2));const pointer=path.resolve(process.argv[3]||'storage/phase5-restored-run.txt');if(!pointer.startsWith(path.join(root,'storage')+path.sep))throw Error('Restore pointer outside storage');fs.writeFileSync(pointer,dir);
 if(!passed)throw Error('Recovery fingerprint mismatch');console.log(JSON.stringify({status:proof.status,database:restored.database,restoredRunDir:dir,referencedFiles:proof.referencedFiles}));
}
main().catch(e=>{console.error(e.message);process.exitCode=1;}).finally(()=>fs.closeSync(fd));
