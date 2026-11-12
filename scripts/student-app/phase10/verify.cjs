const fs=require('fs'),path=require('path'),{spawn}=require('child_process');
const root=path.resolve(__dirname,'../../..'),app=path.resolve(root,'../../language-center-student-app'),web=path.resolve(root,'../../language-center-web-frontend');
const dir=path.join(root,'storage/student-phase10');fs.mkdirSync(dir,{recursive:true});
function run(name,cwd,command){return new Promise(resolve=>{
 const fd=fs.openSync(path.join(dir,name+'.log'),'w');const child=spawn(process.env.ComSpec||'cmd.exe',['/d','/s','/c',command],{cwd,env:{...process.env,MAVEN_OPTS:'-Xmx384m -XX:ActiveProcessorCount=2',JAVA_TOOL_OPTIONS:'-Xmx512m -XX:ActiveProcessorCount=2',NODE_OPTIONS:'--max-old-space-size=1536'},stdio:['ignore',fd,fd],windowsHide:true});
 child.on('error',e=>{fs.writeSync(fd,e.message);fs.closeSync(fd);resolve({name,exitCode:1});});
 child.on('close',code=>{try{fs.closeSync(fd);}catch{}resolve({name,exitCode:code??1});});
});}
(async()=>{
 const report=[];const backend=process.argv.includes('--backend');
 const jobs=backend?['auth-service','academic-service','teaching-service','finance-service','api-gateway'].map(module=>[module+'-tests',path.join(root,module),'mvnw.cmd -Ddebug=false -Dlogging.level.root=ERROR -Dlogging.level.org.hibernate.SQL=OFF test']):[
 ['app-typecheck',app,'npm.cmd run typecheck'],['app-tests',app,'npm.cmd test'],['web-tests',web,'npm.cmd test'],['web-build',web,'npm.cmd run build']];
 for(const [name,cwd,command] of jobs){const result=await run(name,cwd,command);report.push(result);console.log(name+' exit='+result.exitCode);fs.writeFileSync(path.join(dir,backend?'backend-tests.json':'frontend-tests.json'),JSON.stringify(report,null,2));}
 if(report.some(r=>r.exitCode!==0))process.exitCode=1;
})();
