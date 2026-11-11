const fs=require('fs'),path=require('path'),crypto=require('crypto'),{execFileSync}=require('child_process');
const root=path.resolve(__dirname,'../..');process.chdir(root);
const env={...process.env};for(const line of fs.readFileSync('.env.local','utf8').replace(/^\uFEFF/,'').split(/\r?\n/)){const m=line.match(/^(DB_URL|DB_USERNAME|DB_PASSWORD)=(.*)$/);if(m)env[m[1]]=m[2];}
const u=new URL(env.DB_URL.slice(5)),source=u.pathname.slice(1);if(!/^[A-Za-z0-9_]+$/.test(source))throw Error('Unsafe source name');
const bin='C:/Program Files/MySQL/MySQL Server 8.0/bin',args=['--host='+u.hostname,'--port='+(u.port||3306),'--user='+env.DB_USERNAME,'--default-character-set=utf8mb4'];env.MYSQL_PWD=env.DB_PASSWORD;
function cli(name,extra,input){return execFileSync(path.join(bin,name+'.exe'),[...args,...extra],{env,windowsHide:true,encoding:'utf8',input,maxBuffer:64*1024*1024});}
const runId='demo-g4-'+new Date().toISOString().slice(0,10).replaceAll('-','')+'-'+crypto.randomBytes(3).toString('hex');
const dir=path.join(root,'storage/acceptance',runId);fs.mkdirSync(dir,{recursive:true});
const fingerprint=()=>crypto.createHash('sha256').update(cli('mysqldump',['--single-transaction','--no-create-info','--skip-comments','--skip-add-locks','--skip-disable-keys','--skip-extended-insert','--compact','--order-by-primary','--no-tablespaces','--set-gtid-purged=OFF','--hex-blob',source]).replaceAll('\r\n','\n')).digest('hex');
fs.writeFileSync(path.join(dir,'source-before.json'),JSON.stringify({database:source,dataSha256:fingerprint()},null,2));
const database='lc_rehearsal_demo_g4_'+Date.now()+'_'+crypto.randomBytes(2).toString('hex');
if(database===source||!/^lc_rehearsal_demo_g4_\w+$/.test(database))throw Error('Unsafe target');
const ddl=cli('mysqldump',['--no-data','--skip-comments','--no-tablespaces','--set-gtid-purged=OFF',source]);
fs.writeFileSync(path.join(dir,'schema.sql'),ddl);
cli('mysql',['--execute','CREATE DATABASE `'+database+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci']);
cli('mysql',[database],ddl);
const migrations=['prepare-existing-schema.sql',...fs.readdirSync('docs/migrations').filter(f=>/^V\d{3}__.*\.sql$/.test(f)).sort()];
for(const file of migrations)cli('mysql',[database],fs.readFileSync('docs/migrations/'+file,'utf8'));
fs.writeFileSync(path.join(dir,'migrations.json'),JSON.stringify(migrations,null,2));
const tables=cli('mysql',['--batch','--skip-column-names',database,'--execute','SHOW TABLES']).trim().split(/\r?\n/);
for(const t of tables)if(!/^[A-Za-z0-9_]+$/.test(t))throw Error('Unsafe table');
cli('mysql',[database,'--execute',tables.map(t=>'ALTER TABLE `'+t+'` AUTO_INCREMENT=1;').join('\n')]);
const backup=path.join(dir,'bootstrap');const docs=path.join(dir,'documents');fs.mkdirSync(backup);fs.mkdirSync(docs);
fs.writeFileSync(path.join(backup,'rehearsal.json'),JSON.stringify({database,restoredDocuments:docs,method:'Fresh schema-only copy; no source rows restored'}));
const runtime={runId,dir,database,prefix:'DEMO',basePort:28080,frontendPort:25173,browserPort:29388,backup,createdAt:new Date().toISOString()};
fs.writeFileSync(path.join(dir,'runtime.json'),JSON.stringify(runtime,null,2));
fs.writeFileSync(path.join(dir,'bootstrap.private.json'),JSON.stringify({username:'superadmin',password:'Demo-'+crypto.randomBytes(12).toString('hex')+'!'},null,2));
fs.writeFileSync('storage/demo-phase4-latest.txt',dir);console.log(JSON.stringify({runId,database,sourceReadOnly:true,tables:tables.length}));
