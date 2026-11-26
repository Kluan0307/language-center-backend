const fs=require('node:fs'),path=require('node:path'),net=require('node:net');
const {spawn,execFileSync}=require('node:child_process');
const {root,localRuntimeEnvironment,runtimeEnvironment}=require('../identity/database.cjs');
const delay=ms=>new Promise(r=>setTimeout(r,ms));
async function withServices({database='language_center_db',services=['auth','academic'],folder,secret,websiteKey},work){
 const children=[],portOffset=Number(process.env.WEBSITE_VERIFY_PORT_OFFSET||0);
 if(![0,100].includes(portOffset))throw Error('Unsupported isolated port offset');
 const ports=Object.fromEntries(Object.entries({gateway:31180,auth:31181,academic:31182,teaching:31183,finance:31184}).map(([service,port])=>[service,port+portOffset]));
 const bases=Object.fromEntries(Object.entries(ports).map(([s,p])=>[s,'http://127.0.0.1:'+p]));
 const baseEnv=database==='language_center_db'?localRuntimeEnvironment():runtimeEnvironment(database);
 const jwt=secret||baseEnv.JWT_SECRET||fs.readFileSync(path.join(root,'.env.local'),'utf8').match(/^\s*JWT_SECRET=(.*)$/m)?.[1].trim().replace(/^(['"])(.*)\1$/,'$2');
 if(!jwt)throw Error('JWT_SECRET missing; no credential was changed');
 const env={...baseEnv,JWT_SECRET:jwt,AUTH_URL:bases.auth,ACADEMIC_URL:bases.academic,TEACHING_URL:bases.teaching,FINANCE_URL:bases.finance,
  SPRING_PROFILES_ACTIVE:'prod',SPRING_CONFIG_IMPORT:'optional:file:/nonexistent-refresh.properties',CORS_ALLOWED_ORIGINS:'http://localhost:5173',DDL_AUTO:'validate',VNPAY_ENABLED:'false',PAYMENT_JOBS_ENABLED:'false',NOTIFICATION_JOBS_ENABLED:'false',NOTIFICATIONS_PUSH_ENABLED:'false',ACADEMIC_SEED_BRANCHES_ENABLED:'false',AUTH_BOOTSTRAP_ENABLED:'false',DB_POOL_SIZE:'3',...(websiteKey?{WEBSITE_FORM_SECRET:websiteKey}:{}),DOCUMENT_STORAGE:path.resolve(folder,'documents')};
 fs.mkdirSync(folder,{recursive:true});
 try{
  for(const s of services){await new Promise((r,j)=>{const server=net.createServer();server.once('error',j);server.listen(ports[s],'127.0.0.1',()=>server.close(r));});
   const target=path.join(root,(s==='gateway'?'api-gateway':s+'-service')+'/target'),jar=fs.readdirSync(target).find(f=>f.endsWith('.jar')&&!f.endsWith('.original'));if(!jar)throw Error('Build '+s+' first');
   const fd=fs.openSync(path.join(folder,s+'.log'),'w');children.push(spawn('java',['-Xmx256m','-jar',path.join(target,jar),'--server.address=127.0.0.1','--server.port='+ports[s],'--spring.config.import=optional:file:/nonexistent-refresh.properties','--spring.jpa.hibernate.ddl-auto=validate','--debug=false','--logging.level.org.hibernate.SQL=OFF'],{cwd:root,env,windowsHide:true,stdio:['ignore',fd,fd]}));fs.closeSync(fd);
  }
  await Promise.all(services.map(async s=>{for(let i=0;i<120;i++){if(children.some(c=>c.exitCode!==null))throw Error('Service exited; inspect private logs');try{if((await fetch(bases[s]+'/actuator/health',{signal:AbortSignal.timeout(1000)})).ok)return;}catch{}await delay(500);}throw Error(s+' startup timeout');}));
  async function request(service,route,{token,branch=1,body,method,headers={}}={}){const response=await fetch(bases[service]+'/api/v1/'+service+route,{method:method||(body?'POST':'GET'),headers:{'Content-Type':'application/json',...(branch===null?{}:{'X-Branch-Id':String(branch)}),...(token?{Authorization:'Bearer '+token}:{}),...headers},...(body?{body:JSON.stringify(body)}:{}),signal:AbortSignal.timeout(15000)});const text=await response.text();let data;try{data=JSON.parse(text);}catch{data={message:'Non-JSON response'};}return {status:response.status,data};}
  async function login(username,password){password??=username==='superadmin'&&process.env.REFRESH_ADMIN_PASSWORD?process.env.REFRESH_ADMIN_PASSWORD:'123456';const r=await request('auth','/login',{body:{username,password}});if(r.status!==200||!r.data.token)throw Error('Login failed for '+username+' (HTTP '+r.status+'); credentials were not changed');return r.data;}
  return await work({bases,request,login});
 }finally{for(const child of children)if(child.exitCode===null&&child.pid){if(process.platform==='win32')try{execFileSync('taskkill.exe',['/PID',String(child.pid),'/T','/F'],{stdio:'ignore',windowsHide:true});}catch{}else child.kill();}await delay(500);}
}
module.exports={withServices};
