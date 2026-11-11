const fs=require('fs'),path=require('path'),Module=require('module'),crypto=require('crypto'),{execFileSync}=require('child_process');
const root=path.resolve(__dirname,'../..');process.chdir(root);
const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));const dir=fs.readFileSync('storage/phase5-latest.txt','utf8').trim(),runtime=read(path.join(dir,'runtime.json'));
if(!dir.startsWith(path.join(root,'storage/acceptance')+path.sep)||!/^lc_rehearsal_\w+$/.test(runtime.database))throw Error('Private acceptance run required');
if(read(path.join(dir,'acceptance-summary.json')).status!=='PASSED'||read(path.join(dir,'phase5-live-ui.json')).status!=='PASSED')throw Error('Real acceptance must pass before source migration');
for(const line of fs.readFileSync('.env.local','utf8').split(/\r?\n/)){const m=line.match(/^(DB_URL|DB_USERNAME|DB_PASSWORD)=(.*)$/);if(m)process.env[m[1]]=m[2];}
const filename=path.join(root,'scripts/operations.cjs'),source=fs.readFileSync(filename,'utf8'),mod=new Module(filename);mod.filename=filename;mod.paths=module.paths;mod._compile(source.slice(0,source.indexOf('module.exports ='))+'\nmodule.exports={dbConfig,tableCounts,dataFingerprint,sql,verify};',filename);const h=mod.exports,db=h.dbConfig();
async function main(){
 if(db.database===runtime.database||db.database!==read(path.join(runtime.backup,'manifest.json')).database)throw Error('Configured source mismatch');
 const nullableSql="SELECT is_nullable FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='duration'";
 if(await h.sql(db,nullableSql,runtime.database)!=='YES')throw Error('V011 not validated on acceptance schema');
 const before={database:db.database,nullable:await h.sql(db,nullableSql),counts:await h.tableCounts(db),dataSha256:await h.dataFingerprint(db)};
 if(before.nullable==='YES'){
   const file=path.join(dir,'source-v011-migration.json');
   if(!fs.existsSync(file))fs.writeFileSync(file,JSON.stringify({database:db.database,status:'PASSED',applied:false,before,after:before,dataUnchanged:true,scope:'Already V011-compatible; no source DDL or business writes'},null,2));
   console.log('Source already compatible; no DDL needed.');return;
 }
 if(before.nullable!=='NO')throw Error('Unexpected source duration column');
 const backup=path.join(dir,'source-before-v011-backup');const log=fs.openSync(path.join(dir,'source-v011-backup.log'),'a');
 try {execFileSync(process.execPath,['scripts/operations.cjs','backup','--online-rehearsal','--output',backup,'--documents','storage/improvement6-documents'],{cwd:root,windowsHide:true,stdio:['ignore',log,log]});}finally{fs.closeSync(log);}
 await h.verify(backup);const manifest=read(path.join(backup,'manifest.json'));if(before.dataSha256!==manifest.dataSha256)throw Error('Source changed during backup; migration aborted');
 const migration=fs.readFileSync('docs/migrations/V011__verified_duration_plans.sql','utf8');
 const report={database:db.database,status:'RUNNING',backup,backupConsistency:manifest.consistency,migration:'V011__verified_duration_plans.sql',migrationSha256:crypto.createHash('sha256').update(migration).digest('hex'),before,scope:'Relax legacy duration nullability using validated migration; no source business writes'};
 const file=path.join(dir,'source-v011-migration.json');fs.writeFileSync(file,JSON.stringify(report,null,2));
 execFileSync(db.binary('mysql'),[...db.args,'--database='+db.database],{input:'SET SESSION lock_wait_timeout=10;\n'+migration,env:{...process.env,MYSQL_PWD:process.env.DB_PASSWORD},windowsHide:true,stdio:['pipe','ignore','pipe']});
 report.after={nullable:await h.sql(db,nullableSql),counts:await h.tableCounts(db),dataSha256:await h.dataFingerprint(db)};
 report.dataUnchanged=before.dataSha256===report.after.dataSha256&&JSON.stringify(before.counts)===JSON.stringify(report.after.counts);
 report.status=report.after.nullable==='YES'&&report.dataUnchanged?'PASSED':'FAILED';report.finishedAt=new Date().toISOString();fs.writeFileSync(file,JSON.stringify(report,null,2));
 if(report.status!=='PASSED')throw Error('Migration proof failed; retain backup for review');console.log('PASSED: source V011 applied after verified backup; data/accounts unchanged.');
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
