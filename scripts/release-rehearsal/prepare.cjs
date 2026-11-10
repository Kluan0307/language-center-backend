const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),Module=require('node:module'),{execFileSync}=require('node:child_process');
const root=path.resolve(__dirname,'../..');process.chdir(root);
const json=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
const release=fs.realpathSync(process.argv[2]);
if(!release.startsWith(path.join(root,'storage/releases')+path.sep))throw Error('Private release path required');
require('../verify-release.cjs').verify(release);
const sourceRun=fs.realpathSync(process.argv[3]||fs.readFileSync('storage/acceptance/latest.txt','utf8').trim());
if(!sourceRun.startsWith(path.join(root,'storage/acceptance')+path.sep))throw Error('Private source run required');
const parent=json(path.join(sourceRun,'runtime.json')),restore=json(path.join(parent.backup,'rehearsal.json'));
if(json(path.join(sourceRun,'acceptance-summary.json')).status!=='PASSED')throw Error('Source must be an already qualified isolated demo');
if(!/^lc_rehearsal_[A-Za-z0-9_]+$/.test(parent.database))throw Error('Refuse source other than isolated rehearsal');
const runId=new Date().toISOString().slice(0,10).replaceAll('-','')+'-g20-'+crypto.randomBytes(4).toString('hex'),dir=path.join(root,'storage/acceptance',runId);
fs.mkdirSync(dir);
const original={};for(const line of fs.readFileSync('.env.local','utf8').replace(/^\uFEFF/,'').split(/\r?\n/)){const m=line.match(/^(DB_URL|DB_USERNAME|DB_PASSWORD|JWT_SECRET)=(.*)$/);if(m)original[m[1]]=m[2];}
const sourceEnv={...original,DB_URL:original.DB_URL.replace(/^(jdbc:mysql:\/\/[^/]+\/)[^?]+/,'$1'+parent.database)};
const sourceEnvFile=path.join(dir,'source.private.env');fs.writeFileSync(sourceEnvFile,Object.entries(sourceEnv).map(([k,v])=>k+'='+v).join('\n'));
const runtime={runId,dir,prefix:'G20'+crypto.randomBytes(5).toString('hex'),basePort:28080,frontendPort:25173,browserPort:29388,backup:path.join(dir,'backup'),releaseRoot:release,parentRunId:parent.runId,sourceEnvFile,frontendMode:'packaged-static-same-origin',createdAt:new Date().toISOString()};
function opsHelpers(){const filename=path.join(root,'scripts/operations.cjs'),source=fs.readFileSync(filename,'utf8'),m=new Module(filename);m.filename=filename;m.paths=module.paths;m._compile(source.slice(0,source.indexOf('module.exports ='))+'\nmodule.exports={dbConfig,tableCounts,dataFingerprint};',filename);return m.exports;}
async function main(){
 const fd=fs.openSync(path.join(dir,'prepare.log'),'a');
 try{
  const pauseScript="$ErrorActionPreference='Stop';$p=Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -and $_.CommandLine.Replace('\\','/').Contains('"+sourceRun.replaceAll('\\','/').replaceAll("'","''")+"/jars/') };if($p){throw 'Private source runtime must be stopped before writes-paused backup'};Write-Output 'Private source JVM writers stopped'";
  execFileSync('powershell.exe',['-NoProfile','-Command',pauseScript],{stdio:['ignore',fd,fd],windowsHide:true});
  Object.assign(process.env,original);const helpers=opsHelpers(),config=helpers.dbConfig();
  fs.writeFileSync(path.join(dir,'original-source-before.json'),JSON.stringify({database:config.database,rowCounts:await helpers.tableCounts(config),dataSha256:await helpers.dataFingerprint(config)},null,2));
  for(const args of [['backup','--env',sourceEnvFile,'--documents',restore.restoredDocuments,'--writes-paused','--output',runtime.backup],['verify','--backup',runtime.backup],['rehearse','--env',sourceEnvFile,'--backup',runtime.backup]])execFileSync(process.execPath,[path.join(release,'scripts/operations.cjs'),...args],{cwd:release,env:{...process.env,...sourceEnv},stdio:['ignore',fd,fd],windowsHide:true});
  const restored=json(path.join(runtime.backup,'rehearsal.json'));runtime.database=restored.database;
  fs.writeFileSync(path.join(dir,'runtime.json'),JSON.stringify(runtime,null,2));
  fs.copyFileSync('scripts/acceptance/expected.json',path.join(dir,'expected.json'));
  fs.writeFileSync('storage/acceptance/latest.txt',dir);fs.writeFileSync('storage/g20-latest.txt',dir);
  console.log(JSON.stringify({run:dir,database:runtime.database,release,source:parent.database,consistency:json(path.join(runtime.backup,'manifest.json')).consistency}));
 }finally{fs.closeSync(fd);}
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
