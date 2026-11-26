const crypto=require('node:crypto');
const {fs,path,root,query,dump,assert}=require('../identity/refresh-common.cjs');
const {withServices}=require('./local-services.cjs');const {publish}=require('./publish-refresh.cjs');
(async()=>{const database='lc_identity_http_'+Date.now(),folder=path.join(root,'storage',database);let created=false;
try{query(null,'CREATE DATABASE `'+database+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');created=true;query(database,dump());
 const before=Number(query(database,'SELECT COUNT(*) FROM website_edit_events'));
 const result=await withServices({database,folder,secret:crypto.randomBytes(48).toString('base64')},async api=>{const one=await publish(api,database),two=await publish(api,database);assert.ok([...one.results,...two.results].every(x=>x.status==='unchanged'));assert.equal(Number(query(database,'SELECT COUNT(*) FROM website_edit_events')),before);return {status:'passed',passes:2,unchangedPerPass:one.results.length,newEditorialEvents:0,liveDataModified:false,scope:'authenticated publication replay on isolated MySQL copy'};});
 fs.writeFileSync(path.join(root,'docs/course-classification/d8-publication-replay.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result));
}finally{if(created)query(null,'DROP DATABASE `'+database+'`');}})().catch(e=>{console.error(e.message);process.exitCode=1;});
