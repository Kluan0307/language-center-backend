// Packaging/config verification only. Does not load .env.local or connect to project MySQL.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),http=require('node:http');
const {execFileSync,execFile}=require('node:child_process');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'tmp/docker-contract'),checks=[];
fs.mkdirSync(out,{recursive:true});
const services=JSON.parse(fs.readFileSync(path.join(root,'deployment/docker/services.json'),'utf8'));
function pass(name){checks.push(name);}
const dockerfile=fs.readFileSync(path.join(root,'Dockerfile'),'utf8'),ignore=fs.readFileSync(path.join(root,'.dockerignore'),'utf8');
const template=fs.readFileSync(path.join(root,'.env.docker.example'),'utf8');
for(const key of ['DB_PASSWORD','JWT_SECRET','WEBSITE_FORM_SECRET','VNPAY_HASH_SECRET','GEMINI_API_KEY'])assert.match(template,new RegExp('^'+key+'=$','m'));pass('Secret values are blank in Docker template');
assert.match(dockerfile,/USER 10001:10001/);assert.match(dockerfile,/ENTRYPOINT \["java", "-jar", "\/app\/app.jar"\]/);pass('Non-root runtime and Java PID 1 entrypoint');
assert.match(dockerfile,/FROM \$\{MAVEN_IMAGE\} AS build/);assert.match(dockerfile,/FROM \$\{JAVA_IMAGE\} AS runtime/);pass('Separate JDK build and JRE runtime stages');
assert.ok(dockerfile.includes('-Duser.timezone=Asia/Ho_Chi_Minh'));pass('Container JVM uses center timezone');
assert.ok(ignore.startsWith('# Explicit allowlist:'));assert.match(ignore,/^\*\*$/m);assert.match(ignore,/^\*\*\/\.env\.\*$/m);pass('Build context allowlist excludes host environment');
for(const service of services){
 const folder=path.join(root,service.name),pom=fs.readFileSync(path.join(folder,'pom.xml'),'utf8');assert.match(pom,/<java.version>21<\/java.version>/);
 assert.ok(dockerfile.includes(service.name+':'+service.port),'Service/port pair');
 const profile=fs.readFileSync(path.join(folder,'src/main/resources/application-docker.properties'),'utf8');
 assert.match(profile,/server.address=\$\{SERVER_ADDRESS:0.0.0.0\}/);assert.match(profile,/^logging.file.name=$/m);
 assert.ok(!profile.includes('file:../.env.local'));assert.ok(!profile.includes('http://localhost:808'));
 if(service.database){assert.match(profile,/spring.jpa.hibernate.ddl-auto=validate/);assert.match(profile,/spring.datasource.password=\$\{DB_PASSWORD\}/);assert.deepEqual(service.startupDependencies,['mysql']);}
 pass(service.name+' Docker profile, Java version and port match');
}
execFileSync('javac',['--release','21','-d',out,path.join(root,'deployment/docker/Healthcheck.java')],{windowsHide:true,stdio:'pipe'});pass('Readiness probe compiles on Java 21 without dependencies');
async function probe(port){return await new Promise(resolve=>execFile('java',['-cp',out,'Healthcheck'],{env:{...process.env,SERVER_PORT:String(port)},windowsHide:true,timeout:10000},error=>resolve(error?error.code:0)));}
(async()=>{
 for(const [status,body,expected] of [[200,'{"status":"UP"}',0],[200,'{"status":"DOWN"}',1],[503,'{"status":"UP"}',1],[503,'{"status":"DOWN"}',1],[302,'{"status":"UP"}',1]]){
  const server=http.createServer((req,res)=>{assert.equal(req.url,'/actuator/health/readiness');res.writeHead(status,{'Content-Type':'application/json'});res.end(body);});
  await new Promise(r=>server.listen(0,'127.0.0.1',r));
  try{assert.equal(await probe(server.address().port),expected);}finally{await new Promise(r=>server.close(r));}
  pass('Probe returns '+expected+' for HTTP '+status+' '+body);
 }
 assert.equal(await probe('invalid'),1);pass('Probe rejects invalid runtime port');
 const result={checkedAt:new Date().toISOString(),status:'passed',checks,sourceDatabaseTouched:false};fs.mkdirSync(path.join(root,'docs/docker'),{recursive:true});fs.writeFileSync(path.join(root,'docs/docker/contract-validation.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({status:'passed',checks:checks.length}));
})().catch(e=>{console.error(e.stack);process.exitCode=1;});
