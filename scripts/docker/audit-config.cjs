// Reads source configuration only; private runtime environment files are never opened.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../..'),manifest=JSON.parse(fs.readFileSync(path.join(root,'deployment/docker/services.json'),'utf8'));
function walk(folder){return fs.readdirSync(folder,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(folder,e.name)):[path.join(folder,e.name)]);}
const inventory=manifest.map(service=>{
 const resources=path.join(root,service.name,'src/main/resources'),base=fs.readFileSync(path.join(resources,'application.properties'),'utf8'),profile=fs.readFileSync(path.join(resources,'application-docker.properties'),'utf8');
 const pom=fs.readFileSync(path.join(root,service.name,'pom.xml'),'utf8');
 const serverPort=base.match(/^server.port=(\d+)$/m)?.[1]||base.match(/^server.port=\$\{ASSISTANT_PORT:(\d+)\}$/m)?.[1];assert.equal(Number(serverPort),service.port);
 const files=[...fs.readdirSync(resources).filter(f=>/^application.*\.properties$/.test(f)).map(f=>path.join(resources,f)),...walk(path.join(root,service.name,'src/main/java')).filter(f=>f.endsWith('.java'))];
 const refs=new Map();
 for(const file of files){const content=fs.readFileSync(file,'utf8');for(const match of content.matchAll(/\$\{([A-Z][A-Z0-9_]*)(?::[^}]*)?\}/g)){if(!refs.has(match[1]))refs.set(match[1],new Set());refs.get(match[1]).add(path.relative(root,file).replaceAll('\\','/'));}}
 return {...service,javaVersion:pom.match(/<java.version>(.+?)<\/java.version>/)[1],springBoot:pom.match(/<artifactId>spring-boot-starter-parent<\/artifactId>\s*<version>(.+?)<\/version>/)[1],health:'/actuator/health/readiness',readinessIncludes:(profile+'\n'+base).match(/^management.endpoint.health.group.readiness.include=(.+)$/m)?.[1],storage:service.name==='teaching-service'?'/app/data/documents':null,variables:Array.from(refs.entries()).sort(([a],[b])=>a.localeCompare(b)).map(([name,locations])=>({name,secret:/PASSWORD|SECRET|TOKEN|API_KEY/.test(name),locations:[...locations]}))};
});
fs.mkdirSync(path.join(root,'docs/docker'),{recursive:true});fs.writeFileSync(path.join(root,'docs/docker/config-inventory.json'),JSON.stringify({auditedAt:new Date().toISOString(),privateEnvironmentRead:false,services:inventory},null,2));console.log(JSON.stringify({services:inventory.length,privateEnvironmentRead:false}));
