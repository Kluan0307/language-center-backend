// Small read-only end-to-end probe; no credentials or student details in output.
const fs=require('fs'),path=require('path'),crypto=require('crypto');
const base='http://127.0.0.1:18080/api/v1/';
async function main(){
 const login=await fetch(base+'auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'superadmin',password:process.env.LC_SEED_PASSWORD||'123456'}),signal:AbortSignal.timeout(15000)});
 if(!login.ok)throw Error('Login HTTP '+login.status);
 const headers={Authorization:'Bearer '+(await login.json()).token,'Content-Type':'application/json'};
 const config=await fetch(base+'assistant/config',{headers,signal:AbortSignal.timeout(15000)});const configuration=await config.json();console.log(JSON.stringify({configStatus:config.status,configuration}));
 const start=Date.now();const response=await fetch(base+'assistant/chat',{method:'POST',headers,body:JSON.stringify({message:'Toàn trung tâm có bao nhiêu học viên?',scopeMode:'WHOLE_SYSTEM',requestId:crypto.randomUUID()}),signal:AbortSignal.timeout(45000)});
 const body=await response.json();const report={checkedAt:new Date().toISOString(),httpStatus:response.status,elapsedMs:Date.now()-start,response:body};
 fs.writeFileSync(path.resolve(__dirname,'../../storage/assistant-docker-live.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report));if(!response.ok||body.status!=='ANSWER'||!body.sources?.some(s=>s.tool==='get_student_statistics'))throw Error('Assistant did not return a verified statistics answer');
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
