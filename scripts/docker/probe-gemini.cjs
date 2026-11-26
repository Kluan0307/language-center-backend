const c=require('./data-common.cjs');
const text=c.fs.readFileSync(c.path.join(c.workspace,'.env.docker.local'),'utf8');
const value=k=>text.match(new RegExp('^'+k+'=(.*)$','m'))?.[1].trim();
async function main(){const start=Date.now();const r=await fetch('https://generativelanguage.googleapis.com/v1beta/models/'+value('GEMINI_MODEL')+':generateContent',{method:'POST',headers:{'Content-Type':'application/json','x-goog-api-key':value('GEMINI_API_KEY')},body:JSON.stringify({contents:[{parts:[{text:'Reply with the word OK.'}]}],generationConfig:{maxOutputTokens:32,thinkingConfig:{thinkingLevel:'LOW'}}}),signal:AbortSignal.timeout(25000)});const body=await r.json();console.log(JSON.stringify({httpStatus:r.status,elapsedMs:Date.now()-start,error:body.error?{status:body.error.status,message:body.error.message}:null,hasCandidate:!!body.candidates?.length}));}
main().catch(e=>{console.log(JSON.stringify({error:e.name,message:e.message}));process.exitCode=1;});
