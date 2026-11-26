const fs=require('fs'),path=require('path'),{execFileSync}=require('child_process');
const root=path.resolve(__dirname,'../..');
const env={...process.env};
for(const line of fs.readFileSync(path.join(root,'.env.local'),'utf8').replace(/^\uFEFF/,'').split(/\r?\n/)){
 const m=line.match(/^\s*(DB_URL|DB_USERNAME|DB_PASSWORD)=(.*)$/);if(m)env[m[1]]=m[2].trim().replace(/^(['"])(.*)\1$/,'$2');
}
const url=new URL(env.DB_URL.replace(/^jdbc:/,''));
if(!['localhost','127.0.0.1'].includes(url.hostname)||url.pathname!='/language_center_db')throw Error('Expected local language_center_db');
env.MYSQL_PWD=env.DB_PASSWORD;
const executable='C:/Program Files/MySQL/MySQL Server 8.0/bin/';
const connection=['--host='+url.hostname,'--port='+(url.port||3306),'--user='+env.DB_USERNAME];
function query(db,input){return execFileSync(executable+'mysql.exe',[...connection,'--default-character-set=utf8mb4','--batch','--raw','--skip-column-names',...(db?[db]:[])],{env,input,encoding:'utf8',windowsHide:true,maxBuffer:16*1024*1024}).trim();}
function schema(){return execFileSync(executable+'mysqldump.exe',[...connection,'--no-data','--no-tablespaces','--skip-comments','language_center_db'],{env,encoding:'utf8',windowsHide:true,maxBuffer:16*1024*1024});}
function dump(){return execFileSync(executable+'mysqldump.exe',[...connection,'--single-transaction','--no-tablespaces','--skip-comments','--default-character-set=utf8mb4','language_center_db'],{env,encoding:'utf8',windowsHide:true,maxBuffer:64*1024*1024});}
function runtimeEnvironment(db){if(!/^lc_identity_http_\d+$/.test(db))throw Error('Private database only');const target=new URL(url);target.pathname='/'+db;return {...env,DB_URL:'jdbc:'+target.toString()};}
// Credentials remain in process memory; callers must not serialize this environment.
function localRuntimeEnvironment(){return {...env};}
module.exports={query,schema,dump,root,runtimeEnvironment,localRuntimeEnvironment};
