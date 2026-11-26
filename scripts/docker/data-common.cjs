const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict'),{execFileSync}=require('node:child_process');
const root=path.resolve(__dirname,'../..'),workspace=path.resolve(root,'../..'),database='language_center_db';
function docker(args,input,encoding='utf8'){return execFileSync('docker',args,{input,encoding,windowsHide:true,stdio:['pipe','pipe','pipe'],timeout:240000,maxBuffer:128*1024*1024});}
const prefix=['compose','--project-name','language-center','-f',path.join(root,'compose.yaml'),'-f',path.join(root,'compose.web.yaml'),'--env-file',path.join(workspace,'.env.docker.local'),'--profile','assistant'];
const compose=(args,input)=>docker([...prefix,...args],input).trim();
function mysql(sql,db=database){assert.ok(db===database||/^lc_dck[56]_restore_\d+$/.test(db),'Unexpected database');return compose(['exec','-T','mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --user=root --default-character-set=utf8mb4 --batch --raw --skip-column-names "$@"','mysql',db],"SET time_zone='+07:00';\n"+sql);}
const sha=data=>crypto.createHash('sha256').update(data).digest('hex');
const identifier=name=>{assert.match(name,/^[a-zA-Z0-9_]+$/);return '`'+name+'`';};
function snapshot(query,db=database){
 // Two round trips instead of three docker-exec calls per table. JSON values
 // remain MySQL's canonical strings, preserving the DCK5 fingerprint format.
 const metadata=query(db,"SELECT c.table_name,c.column_name,c.data_type FROM information_schema.columns c JOIN information_schema.tables t ON t.table_schema=c.table_schema AND t.table_name=c.table_name WHERE c.table_schema=DATABASE() AND t.table_type='BASE TABLE' ORDER BY c.table_name,c.ordinal_position");
 const grouped=new Map();for(const line of metadata.split(/\r?\n/).filter(Boolean)){const [table,column,type]=line.split('\t');if(!grouped.has(table))grouped.set(table,[]);grouped.get(table).push([column,type]);}
 const data=new Map([...grouped.keys()].map(name=>[name,[]]));
 const sql=[...grouped].map(([name,columns])=>{
  const expressions=columns.map(([n,t])=>/^(binary|varbinary|blob|tinyblob|mediumblob|longblob|bit)$/.test(t)?'HEX('+identifier(n)+')':identifier(n));
  identifier(name);return "SELECT '"+name+"',JSON_ARRAY("+expressions.join(',')+') FROM '+identifier(name)+';';
 }).join('\n');
 if(sql){const output=query(db,'SET TRANSACTION ISOLATION LEVEL REPEATABLE READ; START TRANSACTION WITH CONSISTENT SNAPSHOT;\n'+sql+'\nCOMMIT;');for(const line of output.replace(/\r\n/g,'\n').split('\n').filter(Boolean)){const tab=line.indexOf('\t');assert.ok(tab>0,'Malformed snapshot row');data.get(line.slice(0,tab)).push(line.slice(tab+1));}}
 const tables=[...grouped].map(([name,columns])=>({name,rows:data.get(name).length,sha256:sha(data.get(name).sort().join('\n')),columns:columns.map(c=>c[0])}));
 return {tables,sha256:sha(JSON.stringify(tables)),rowCount:tables.reduce((n,t)=>n+t.rows,0)};
}
function dockerSnapshot(){return snapshot((db,sql)=>mysql(sql,db));}
function context(){
 const ids=compose(['ps','--all','--quiet']).split(/\r?\n/).filter(Boolean),states=JSON.parse(docker(['inspect',...ids]));
 const mysqlState=states.find(c=>c.Config.Labels['com.docker.compose.service']==='mysql');
 assert.ok(mysqlState?.State.Running&&mysqlState.State.Health.Status==='healthy','Docker MySQL not healthy');
 assert.equal(mysqlState.Config.Labels['com.docker.compose.project'],'language-center');
 const teaching=states.find(c=>c.Config.Labels['com.docker.compose.service']==='teaching-service');
 const volume=teaching.Mounts.find(m=>m.Destination==='/app/data/documents');assert.equal(volume.Type,'volume');assert.equal(volume.Name,'language-center_documents');
 return {states,documentsVolume:volume.Name};
}
function volumeArchive(volume){assert.equal(volume,'language-center_documents');return docker(['run','--rm','--network','none','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges:true','--user','10001:10001','--mount','type=volume,source='+volume+',target=/data,readonly','--entrypoint','tar','eclipse-temurin:21-jre-jammy','-C','/data','-cf','-','.'],undefined,null);}
function restoreFiles(volume,archive){assert.equal(volume,'language-center_documents');docker(['run','--rm','-i','--network','none','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges:true','--user','10001:10001','--mount','type=volume,source='+volume+',target=/data','--entrypoint','tar','eclipse-temurin:21-jre-jammy','-C','/data','-xf','-'],archive,null);}
function files(folder){
 if(!fs.existsSync(folder))return [];
 const result=[];function walk(current){for(const e of fs.readdirSync(current,{withFileTypes:true})){assert.ok(!e.isSymbolicLink(),'Document symlinks are not allowed');const full=path.join(current,e.name);if(e.isDirectory())walk(full);else if(e.isFile())result.push({name:path.relative(folder,full).replaceAll('\\','/'),bytes:fs.statSync(full).size,sha256:sha(fs.readFileSync(full))});}}
 walk(folder);return result.sort((a,b)=>a.name.localeCompare(b.name));
}
module.exports={fs,path,assert,root,workspace,database,docker,compose,mysql,sha,snapshot,dockerSnapshot,context,volumeArchive,restoreFiles,files,identifier,execFileSync};
