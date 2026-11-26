const {spawnSync}=require('node:child_process'),net=require('node:net');
const {root,query}=require('../identity/database.cjs');
const steps=[['scripts/identity/refresh-inventory.cjs'],['scripts/identity/refresh-plan.cjs'],['scripts/identity/refresh-backup.cjs'],['scripts/identity/refresh-seed.cjs'],['scripts/identity/refresh-run.cjs'],['scripts/identity/refresh-run.cjs','--apply-local'],['scripts/website/publish-refresh.cjs','--apply-local']];
(async()=>{
 if(!process.argv.includes('--reset-and-publish-local'))return console.log(JSON.stringify({mode:'dry-run',database:'localhost/language_center_db',preserve:'accounts, credentials, personnel, memberships, branches and configuration',replace:'business dataset and website editorial metadata',steps:steps.map(s=>'node '+s.join(' ')),currentCourses:Number(query('language_center_db','SELECT COUNT(*) FROM courses')),writesPerformed:false},null,2));
 // Do not race the standard services or the dedicated acceptance/publishing runners.
 for(const port of [8080,8081,8082,8083,8084,8086,31180,31181,31182,31183,31184])await new Promise((resolve,reject)=>{const s=net.createServer();s.once('error',()=>reject(Error('Port '+port+' is in use. Stop backend services before recreating the local demo.')));s.listen(port,'127.0.0.1',()=>s.close(resolve));});
 for(const args of steps){console.log('Running '+args.join(' '));const r=spawnSync(process.execPath,args,{cwd:root,env:process.env,stdio:'inherit',windowsHide:true});if(r.status!==0)throw Error('Stopped at '+args[0]+'. Earlier stages were recorded; inspect their reports before continuing.');}
 console.log('Local data recreated and CMS publications prepared. Reload clients and sign in again.');
})().catch(e=>{console.error(e.message);process.exitCode=1;});
