const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{isDeepStrictEqual}=require('node:util'),{execFileSync}=require('node:child_process');
const root=path.resolve(__dirname,'../..'),workspace=path.resolve(root,'../..'),envFile=path.join(workspace,'.env.docker.local');
const docker=(args,input)=>execFileSync('docker',args,{input,encoding:'utf8',stdio:['pipe','pipe','pipe'],windowsHide:true,timeout:120000,maxBuffer:8*1024*1024}).trim();
const prefix=['compose','--project-name','language-center','--file',path.join(root,'compose.yaml'),'--env-file',envFile,'--profile','assistant'];
const keepAlive=setInterval(()=>{},1000);
(async()=>{try{
 const config=JSON.parse(docker([...prefix,'config','--format','json']));
 const wrapper=JSON.parse(docker(['compose','--file',path.join(workspace,'compose.yaml'),'--profile','assistant','config','--format','json']));
 for(const key of ['services','networks','volumes'])for(const name of Object.keys(config[key]))assert.ok(isDeepStrictEqual(wrapper[key]?.[name],config[key][name]),'Root Compose wrapper mismatch: '+key+'.'+name);
 const ids=docker([...prefix,'ps','--all','--quiet']).split(/\r?\n/).filter(Boolean),states=JSON.parse(docker(['inspect',...ids])).filter(c=>Object.hasOwn(config.services,c.Config.Labels['com.docker.compose.service']));assert.equal(states.length,7);assert.ok(states.every(c=>c.State.Running&&c.State.Health.Status==='healthy'));
 const address=docker([...prefix,'port','api-gateway','8080']).split(/\r?\n/)[0],response=await fetch('http://'+address+'/actuator/health/readiness',{signal:AbortSignal.timeout(5000)});assert.equal(response.status,200);assert.equal((await response.json()).status,'UP');
 const sql="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE(); SELECT COUNT(*) FROM accounts; SELECT COUNT(*) FROM students; SELECT COUNT(*) FROM courses;";
 const counts=docker([...prefix,'exec','-T','mysql','sh','-c','MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --user="$MYSQL_USER" --batch --skip-column-names "$MYSQL_DATABASE"'],sql).split(/\r?\n/).map(Number);assert.equal(counts[0],88);assert.ok(counts.slice(1).every(n=>Number.isInteger(n)&&n>=0));
 const result={checkedAt:new Date().toISOString(),status:'passed',project:'language-center',wrapperMatches:true,gateway:'http://'+address,tableCount:counts[0],accounts:counts[1],students:counts[2],courses:counts[3],services:states.map(c=>({service:c.Config.Labels['com.docker.compose.service'],state:c.State.Status,health:c.State.Health.Status})),sourceDatabaseWritten:false};
 fs.writeFileSync(path.join(root,'docs/docker/runtime-backend-validation.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({status:'passed',services:states.length,gateway:result.gateway,tableCount:counts[0],accounts:counts[1],students:counts[2],courses:counts[3]}));
}finally{clearInterval(keepAlive);}})().catch(e=>{console.error(e.message);process.exitCode=1;});
