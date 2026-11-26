// Inspects the six local images; starts only gateway/assistant without project DB/provider secrets.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {execFileSync,spawnSync}=require('node:child_process');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'tmp/docker-images'),tag=process.env.DOCKER_IMAGE_TAG||'local';
assert.match(tag,/^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$/);
fs.mkdirSync(out,{recursive:true});
const catalog=JSON.parse(fs.readFileSync(path.join(root,'deployment/docker/services.json'),'utf8'));
const owned=new Set(),images=[],checks=[],smoke=[];
const javaInfo=spawnSync('java',['-XshowSettings:properties','-version'],{encoding:'utf8',windowsHide:true});
const javaHome=javaInfo.stderr?.match(/^\s*java.home\s*=\s*(.+)$/m)?.[1].trim();
assert.ok(javaHome,'Java runtime not found');
const jarTool=path.join(javaHome,'bin',process.platform==='win32'?'jar.exe':'jar');
assert.ok(fs.existsSync(jarTool),'JDK jar tool not found');
const docker=(args)=>execFileSync('docker',args,{encoding:'utf8',windowsHide:true,timeout:120000,maxBuffer:8*1024*1024}).trim();
const delay=ms=>new Promise(r=>setTimeout(r,ms));
function pass(name){checks.push(name);}
function name(service){return 'lc-dck2-'+crypto.randomUUID().slice(0,8)+'-'+service;}
function remove(container){try{docker(['rm','--force',container]);}finally{owned.delete(container);}}
const keepAlive=setInterval(()=>{},1000);
(async()=>{try{
 assert.equal(docker(['info','--format','{{.OSType}}']),'linux');
 for(const service of catalog){
  const image='language-center/'+service.name+':'+tag,data=JSON.parse(docker(['image','inspect',image]))[0];
  assert.equal(data.Config.User,'10001:10001');assert.deepEqual(data.Config.Entrypoint,['java','-jar','/app/app.jar']);
  const environment=Object.fromEntries(data.Config.Env.map(value=>{const at=value.indexOf('=');return [value.slice(0,at),value.slice(at+1)];}));
  assert.equal(environment.SERVER_PORT,String(service.port));assert.equal(environment.SERVICE_NAME,service.name);assert.equal(environment.SPRING_PROFILES_ACTIVE,'prod,docker');assert.equal(environment.SERVER_ADDRESS,'0.0.0.0');
  assert.equal(environment.TZ,'Asia/Ho_Chi_Minh');assert.ok(environment.JAVA_TOOL_OPTIONS.includes('-Duser.timezone=Asia/Ho_Chi_Minh'));
  assert.ok(data.Config.ExposedPorts[service.port+'/tcp']);
  assert.deepEqual(data.Config.Healthcheck.Test,['CMD','java','-cp','/app/health','Healthcheck']);
  for(const key of ['DB_PASSWORD','JWT_SECRET','WEBSITE_FORM_SECRET','GEMINI_API_KEY','VNPAY_HASH_SECRET'])assert.ok(!(key in environment),'Secret baked into image: '+key);
  pass(service.name+' metadata: non-root, PID1, profile, port, health and no baked secrets');
  const proof=docker(['run','--rm','--network','none','--entrypoint','sh',image,'-c','test ! -e /app/.env.local && test ! -e /app/src && test ! -e /app/pom.xml && test -f /app/health/Healthcheck.class && touch /app/data/documents/.probe && id -u']);assert.equal(proof,'10001');
  pass(service.name+' runtime filesystem is minimal and document path is writable by app user');
  const container=name(service.name+'-jar');owned.add(container);docker(['create','--name',container,'--network','none',image]);
  const jar=path.join(out,service.name+'.jar');docker(['cp',container+':/app/app.jar',jar]);remove(container);
  const entries=execFileSync(jarTool,['tf',jar],{encoding:'utf8',windowsHide:true,maxBuffer:8*1024*1024});
  assert.ok(entries.includes('BOOT-INF/classes/application-docker.properties'));assert.ok(entries.includes('BOOT-INF/classes/application.properties'));
  assert.ok(!entries.split(/\r?\n/).some(e=>/(^|\/)\.env(?:\.|$)|(^|\/)storage\//.test(e)));
  pass(service.name+' executable JAR contains Docker profile and no host environment/storage');
  images.push({service:service.name,image,id:data.Id,size:data.Size,user:data.Config.User,port:service.port,healthcheck:true,jarSha256:crypto.createHash('sha256').update(fs.readFileSync(jar)).digest('hex')});
  fs.unlinkSync(jar);
 }
 for(const service of catalog.filter(s=>!s.database)){
  const container=name(service.name),image='language-center/'+service.name+':'+tag;owned.add(container);
  docker(['run','--detach','--name',container,'--memory','512m','--cpus','1','--publish','127.0.0.1::'+service.port,'--env','ASSISTANT_CHAT_ENABLED=false','--env','ASSISTANT_MCP_ENABLED=false',image]);
  const address=docker(['port',container,service.port+'/tcp']).split(/\r?\n/)[0],base='http://'+address;
  let healthy=false;
  for(let attempt=0;attempt<180;attempt++){
   const state=JSON.parse(docker(['inspect',container]))[0].State;
   if(!state.Running)throw Error(service.name+' exited during startup; inspect test container logs');
   try{const response=await fetch(base+'/actuator/health/readiness',{signal:AbortSignal.timeout(1000)});if(response.ok&&(await response.json()).status==='UP'){healthy=true;break;}}catch{}
   await delay(300);
  }
  assert.ok(healthy,service.name+' readiness timeout');pass(service.name+' runs with prod,docker and responds through published loopback port');
  docker(['exec',container,'java','-cp','/app/health','Healthcheck']);pass(service.name+' in-container readiness probe succeeds');
  const blocked=await fetch(base+'/actuator/env',{signal:AbortSignal.timeout(3000)});assert.ok([401,403,404].includes(blocked.status));pass(service.name+' does not expose actuator environment');
  const logs=docker(['logs',container]);assert.ok(logs.includes('"prod", "docker"')||logs.includes('prod, docker'),'Profiles not reported active');
  docker(['stop','--time','40',container]);const stopped=JSON.parse(docker(['inspect',container]))[0].State;assert.equal(stopped.OOMKilled,false);assert.ok([0,143].includes(stopped.ExitCode),'Graceful shutdown failed');pass(service.name+' handles SIGTERM without forced kill');
  smoke.push({service:service.name,readiness:true,publishedPort:true,probe:true,protectedActuator:true,exitCode:stopped.ExitCode});remove(container);
 }
 const bases=['maven:3-eclipse-temurin-21-jammy','eclipse-temurin:21-jre-jammy'].map(image=>{const data=JSON.parse(docker(['image','inspect',image]))[0];return {image,digests:data.RepoDigests};});
 const result={checkedAt:new Date().toISOString(),status:'passed',checks,images,bases,smoke,sourceDatabaseTouched:false,limitations:['Four DB services inspected as images/JARs; MySQL/container-network integration is DCK3/DCK5','No Compose or database restore performed','Business Maven tests skipped for packaging builds']};
 fs.mkdirSync(path.join(root,'docs/docker'),{recursive:true});fs.writeFileSync(path.join(root,'docs/docker/image-validation.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({status:'passed',images:images.length,checks:checks.length,smoke},null,2));
}finally{clearInterval(keepAlive);for(const container of owned)try{docker(['rm','--force',container]);}catch{}}})().catch(e=>{fs.writeFileSync(path.join(out,'failure.json'),JSON.stringify({message:e.message,checks},null,2));console.error(e.stack);process.exitCode=1;});
