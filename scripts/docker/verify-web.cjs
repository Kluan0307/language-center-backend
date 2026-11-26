// DCK4 acceptance against the running local stack; never inserts application data.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{isDeepStrictEqual}=require('node:util'),{execFileSync}=require('node:child_process');
const root=path.resolve(__dirname,'../..'),workspace=path.resolve(root,'../..');
const docker=(args,input)=>execFileSync('docker',args,{input,encoding:'utf8',stdio:['pipe','pipe','pipe'],windowsHide:true,timeout:120000,maxBuffer:12*1024*1024}).trim();
const prefix=['compose','--project-name','language-center','-f',path.join(root,'compose.yaml'),'-f',path.join(root,'compose.web.yaml'),'--env-file',path.join(workspace,'.env.docker.local'),'--profile','assistant'];
const checks=[],keepAlive=setInterval(()=>{},1000);
const check=(name,ok)=>{assert.ok(ok,name);checks.push(name);};
const request=(url,options={})=>fetch(url,{...options,signal:AbortSignal.timeout(20000)});
async function run(){
 const config=JSON.parse(docker([...prefix,'config','--format','json']));
 const wrapper=JSON.parse(docker(['compose','-f',path.join(workspace,'compose.yaml'),'--profile','assistant','config','--format','json']));
 for(const key of ['services','networks','volumes'])check('Root wrapper matches '+key,isDeepStrictEqual(config[key],wrapper[key]));
 const states=JSON.parse(docker(['inspect',...docker([...prefix,'ps','--all','--quiet']).split(/\r?\n/).filter(Boolean)]));
 check('All nine containers healthy',states.length===9&&states.every(c=>c.State.Running&&c.State.Health?.Status==='healthy'));
 const address=async(name,port)=>'http://'+docker([...prefix,'port',name,String(port)]).split(/\r?\n/)[0];
 const admin=await address('web-admin',8080),site=await address('website',3000),gateway=await address('api-gateway',8080);
 const home=await request(admin);check('Admin root serves HTML',home.status===200&&/text\/html/.test(home.headers.get('content-type')));
 const markup=await home.text();check('HTML is not permanently cached',home.headers.get('cache-control')==='no-store');
 check('Admin liveness', (await request(admin+'/healthz')).status===200);
 const deep=await request(admin+'/admin/students');check('SPA deep links resolve index',deep.status===200&&(await deep.text())===markup);
 check('Missing asset returns 404',(await request(admin+'/assets/missing-dck4.js')).status===404);
 const assets=[...markup.matchAll(/(?:src|href)="([^\"]+\.js)"/g)].map(m=>m[1]);
 const asset=assets[0],js=await request(admin+asset),bundle=(await Promise.all(assets.map(async p=>(await request(admin+p)).text()))).join('\n');
 check('Hashed JS cached immutable',js.status===200&&/immutable/.test(js.headers.get('cache-control')));
 check('Browser bundle uses same-origin API',bundle.includes('/api/v1')&&!bundle.includes('http://localhost:8080/api/v1'));
 const login=await request(admin+'/api/v1/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'dck4-missing-user',password:'invalid'})});
 check('Auth request reaches backend',login.status===401&&/json/.test(login.headers.get('content-type')));
 const unknown=await request(admin+'/api/v1/academic/public/dck4-missing');check('API errors never fall back to SPA',unknown.status>=400&&!/text\/html/.test(unknown.headers.get('content-type')||''));
 const catalog=await request(admin+'/api/v1/academic/public/catalog/COURSE');check('Admin proxy reaches catalog',catalog.status===200&&Array.isArray((await catalog.json()).items));
 for(const origin of ['http://localhost:15173','http://localhost:13000']){const r=await request(gateway+'/api/v1/auth/login',{method:'OPTIONS',headers:{Origin:origin,'Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'content-type'}});check('Gateway allows '+origin,r.ok&&r.headers.get('access-control-allow-origin')===origin);}
 const foreign=await request(gateway+'/api/v1/auth/login',{method:'OPTIONS',headers:{Origin:'https://foreign.invalid','Access-Control-Request-Method':'POST'}});check('Foreign CORS denied',!foreign.headers.get('access-control-allow-origin'));
 for(const route of ['/api/health?check=ready','/','/khoa-hoc','/chuong-trinh','/chi-nhanh','/giao-vien','/tu-van','/robots.txt','/sitemap.xml'])check('Website HTTP 200 '+route,(await request(site+route)).status===200);
 const nextHome=await(await request(site)).text();check('Vietnamese HTML and local canonical',nextHome.includes('lang="vi"')&&nextHome.includes('http://localhost:13000'));
 check('Local website not indexable',/Disallow: \/\s/.test(await(await request(site+'/robots.txt')).text()));
 const listing=await request(site+'/api/catalog?kind=COURSE');check('Next BFF reaches Docker gateway',listing.status===200&&Array.isArray((await listing.json()).items));
 const form=(origin,body)=>request(site+'/api/consultations',{method:'POST',headers:{Origin:origin,'Content-Type':'application/json'},body});
 check('Form key accepted by backend; invalid data rejected',(await form(site,'{}')).status===400);
 check('Foreign form origin rejected',(await form('https://foreign.invalid','{}')).status===403);
 check('Form body limit enforced',(await form(site,JSON.stringify({note:'x'.repeat(9000)}))).status===413);
 for(const name of ['web-admin','website']){const state=states.find(c=>c.Config.Labels['com.docker.compose.service']===name);check(name+' nonroot/read-only/capabilities',!['','0','root'].includes(state.Config.User)&&state.HostConfig.ReadonlyRootfs&&state.HostConfig.CapDrop.includes('ALL'));const image=JSON.parse(docker(['image','inspect',state.Image]))[0];check(name+' image has no runtime secrets',!(image.Config.Env||[]).some(v=>/^(WEBSITE_FORM_SECRET|DB_PASSWORD|JWT_SECRET)=/.test(v)));}
 const scan="const fs=require('fs');let count=0;function walk(p){for(const e of fs.readdirSync(p,{withFileTypes:true})){const n=p+'/'+e.name;if(e.isDirectory())walk(n);else{const b=fs.readFileSync(n);if(process.env.WEBSITE_FORM_SECRET&&b.includes(Buffer.from(process.env.WEBSITE_FORM_SECRET)))throw Error('Private key found in browser assets');count++;}}}walk('/app/.next/static');console.log(count);";
 check('Website private key absent from browser assets',Number(docker([...prefix,'exec','-T','website','node','-e',scan]))>0);
 const sql='SELECT COUNT(*) FROM accounts; SELECT COUNT(*) FROM students; SELECT COUNT(*) FROM courses; SELECT COUNT(*) FROM leads;';
 const rows=docker([...prefix,'exec','-T','mysql','sh','-c','MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --user="$MYSQL_USER" --batch --skip-column-names "$MYSQL_DATABASE"'],sql).split(/\r?\n/).map(Number);
 check('Docker business table counts readable',rows.every(n=>Number.isInteger(n)&&n>=0));
 const result={checkedAt:new Date().toISOString(),status:'passed',checks,urls:{admin,site,gateway},services:states.map(c=>({name:c.Config.Labels['com.docker.compose.service'],health:c.State.Health.Status})),sourceDatabaseWritten:false,data:Object.fromEntries(['accounts','students','courses','leads'].map((name,i)=>[name,rows[i]])),limitations:['Physical student device not connected.']};
 fs.writeFileSync(path.join(root,'docs/docker/runtime-web-validation.json'),JSON.stringify(result,null,2));
 console.log(JSON.stringify({status:'passed',checks:checks.length,urls:result.urls}));
}
run().catch(e=>{console.error(e.message);process.exitCode=1;}).finally(()=>clearInterval(keepAlive));
