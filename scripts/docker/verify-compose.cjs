// DCK3 integration on a uniquely named Compose project. Never imports source application rows.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const {execFileSync,spawn}=require('node:child_process');
const root=path.resolve(__dirname,'../..'),project='lc-dck3-'+Date.now(),folder=path.join(root,'tmp',project),checks=[];
fs.mkdirSync(folder,{recursive:true});
let template=fs.readFileSync(path.join(root,'.env.docker.example'),'utf8');
for(const key of ['DB_PASSWORD','MYSQL_ROOT_PASSWORD','JWT_SECRET','WEBSITE_FORM_SECRET'])template=template.replace(new RegExp('^'+key+'=$','m'),key+'='+crypto.randomBytes(32).toString('hex'));
template=template.replace(/^GATEWAY_HOST_PORT=.*$/m,'GATEWAY_HOST_PORT=0');
const envFile=path.join(folder,'.env.docker.local');fs.writeFileSync(envFile,template,{mode:0o600});
const prefix=['compose','--project-name',project,'--file',path.join(root,'compose.yaml'),'--env-file',envFile];
const docker=(args,input)=>execFileSync('docker',args,{input,encoding:'utf8',stdio:['pipe','pipe','pipe'],windowsHide:true,maxBuffer:8*1024*1024,timeout:120000}).trim();
const command=(args,input)=>docker([...prefix,...args],input);
const pass=name=>checks.push(name);
async function run(args){const log=fs.openSync(path.join(folder,'compose.log'),'a');try{await new Promise((resolve,reject)=>{const child=spawn('docker',[...prefix,...args],{windowsHide:true,stdio:['ignore',log,log]});child.on('error',reject);child.on('exit',code=>code===0?resolve():reject(Error('Compose operation failed; inspect '+path.join(folder,'compose.log'))));});}finally{fs.closeSync(log);}}
function sql(query,admin=false){return command(['exec','-T','mysql','sh','-c',admin?'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --user=root --batch --skip-column-names "$MYSQL_DATABASE"':'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --user="$MYSQL_USER" --batch --skip-column-names "$MYSQL_DATABASE"'],query);}
const keepAlive=setInterval(()=>{},1000);let created=false,result;
(async()=>{try{
 const config=JSON.parse(command(['--profile','assistant','config','--format','json']));
 assert.equal(Object.keys(config.services).length,7);pass('Compose declares MySQL and six backend services');
 assert.ok(!config.services.mysql.ports);assert.equal(config.networks.database.internal,true);pass('Database has an internal network and no published port');
 for(const [name,service] of Object.entries(config.services)){
  if(!['api-gateway','mysql'].includes(name))assert.ok(!service.ports,'Unexpected exposed service '+name);
  assert.equal(service.logging.options['max-size'],'10m');
  if(name!=='mysql')assert.ok(!('MYSQL_ROOT_PASSWORD' in service.environment),'Root secret leaked to service '+name);
 }
 pass('Only gateway is published and all containers have bounded logs');
 for(const name of ['api-gateway','assistant-service'])for(const key of ['DB_PASSWORD','JWT_SECRET','WEBSITE_FORM_SECRET'])assert.ok(!(key in config.services[name].environment));
 pass('Gateway/Assistant do not receive DB or signing secrets');
 assert.ok(!config.services['auth-service'].environment.GEMINI_API_KEY);assert.ok(config.services['assistant-service'].profiles.includes('assistant'));pass('AI credentials stay on optional Assistant profile');
 created=true;await run(['up','--detach','--wait','--wait-timeout','240','--no-build']);
 let ids=command(['ps','--all','--quiet']).split(/\r?\n/).filter(Boolean),states=JSON.parse(docker(['inspect',...ids]));
 assert.equal(states.length,6);assert.ok(states.every(c=>c.State.Running&&c.State.Health.Status==='healthy'));pass('Default project starts five core services and MySQL healthy');
 assert.ok(states.every(c=>c.Config.Labels['com.docker.compose.project']===project));
 const tableCount=Number(sql('SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();'));assert.equal(tableCount,88);pass('Empty Docker MySQL contains all 88 expected tables');
 const tables=sql('SHOW TABLES;').split(/\r?\n/);for(const table of tables){assert.match(table,/^[a-z0-9_]+$/);assert.equal(Number(sql('SELECT COUNT(*) FROM `'+table+'`;')),0);}
 pass('Schema initialization imports zero application rows and seed remains disabled');
 let refused=false;try{sql('CREATE TABLE dck3_forbidden(id INT);');}catch{refused=true;}assert.equal(refused,true);pass('Runtime DB account cannot create schema objects');
 const base='http://'+command(['port','api-gateway','8080']).split(/\r?\n/)[0];
 const health=await fetch(base+'/actuator/health/readiness');assert.equal(health.status,200);assert.equal((await health.json()).status,'UP');pass('Gateway readiness reachable on loopback mapped port');
 const catalog=await fetch(base+'/api/v1/academic/public/catalog/COURSE');assert.equal(catalog.status,200);assert.deepEqual((await catalog.json()).items,[]);pass('Gateway resolves Academic DNS and returns the empty public catalog');
 const login=await fetch(base+'/api/v1/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'not-created',password:'not-created'})});assert.equal(login.status,401);pass('Gateway reaches Auth and rejects unknown credentials');
 for(const route of ['/api/v1/teaching/exams','/api/v1/finance/tuitions']){const response=await fetch(base+route);assert.equal(response.status,401);}
 pass('Gateway reaches protected Teaching and Finance routes');
 command(['exec','-T','teaching-service','sh','-c','test "$(id -u)" = 10001 && printf persisted > /app/data/documents/.dck3-volume-probe']);pass('Document volume writable by UID 10001');
 sql('CREATE TABLE dck3_volume_probe(id INT PRIMARY KEY,value VARCHAR(32)); INSERT INTO dck3_volume_probe VALUES(1,\'persisted\');',true);
 await run(['up','--detach','--no-build','--no-deps','--force-recreate','--wait','--wait-timeout','180','mysql']);
 assert.equal(sql('SELECT value FROM dck3_volume_probe WHERE id=1;'),'persisted');pass('MySQL data persists after container recreation');
 await run(['up','--detach','--no-build','--no-deps','--force-recreate','--wait','--wait-timeout','180','teaching-service']);
 assert.equal(command(['exec','-T','teaching-service','cat','/app/data/documents/.dck3-volume-probe']),'persisted');pass('Teaching documents persist after container recreation');
 await run(['--profile','assistant','up','--detach','--wait','--wait-timeout','240','--no-build']);
 ids=command(['--profile','assistant','ps','--all','--quiet']).split(/\r?\n/).filter(Boolean);states=JSON.parse(docker(['inspect',...ids]));assert.equal(states.length,7);assert.ok(states.every(c=>c.State.Health.Status==='healthy'));pass('Optional Assistant profile starts without provider credentials');
 assert.equal((await fetch(base+'/api/v1/assistant/config')).status,401);pass('Gateway resolves Assistant DNS and preserves authentication');
 result={checkedAt:new Date().toISOString(),status:'passed',checks,tableCount,rowsImported:0,providersCalled:false,sourceDatabaseWritten:false,services:states.map(c=>({name:c.Config.Labels['com.docker.compose.service'],health:c.State.Health.Status})),project};
}finally{
 clearInterval(keepAlive);
 if(created){assert.match(project,/^lc-dck3-\d+$/);await run(['--profile','assistant','down','--volumes','--remove-orphans']);}
 fs.unlinkSync(envFile);
}
if(result){result.temporaryProjectRemoved=true;fs.writeFileSync(path.join(root,'docs/docker/dck3-validation.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({status:result.status,checks:checks.length,services:result.services.length,tableCount:result.tableCount,temporaryProjectRemoved:true}));}
})().catch(e=>{fs.writeFileSync(path.join(folder,'failure.json'),JSON.stringify({message:e.message,checks},null,2));console.error(e.stack);process.exitCode=1;});
