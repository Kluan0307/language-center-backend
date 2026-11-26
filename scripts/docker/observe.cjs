// Local read-only monitoring and a small reproducible API baseline.
const c=require('./data-common.cjs'),fs=require('fs'),path=require('path');
const keepAlive=setInterval(()=>{},1000);
(async()=>{
 const urls={catalog:'http://127.0.0.1:18080/api/v1/academic/public/catalog/COURSE?size=20',website:'http://127.0.0.1:13000/api/catalog?kind=COURSE'};
 const measurements=[];
 for(const [name,url] of Object.entries(urls)){
  for(let i=0;i<3;i++){const r=await fetch(url,{signal:AbortSignal.timeout(15000)});await r.arrayBuffer();}
  const samples=[];let errors=0;
  await Promise.all(Array.from({length:4},async()=>{for(let i=0;i<10;i++){const start=performance.now();try{const r=await fetch(url,{signal:AbortSignal.timeout(15000)});await r.arrayBuffer();if(!r.ok)errors++;}catch{errors++;}samples.push(performance.now()-start);}}));
  samples.sort((a,b)=>a-b);measurements.push({name,requests:40,concurrency:4,errors,p50Ms:Math.round(samples[19]),p95Ms:Math.round(samples[37]),maxMs:Math.round(samples[39])});
 }
 const stats=c.docker(['stats','--no-stream','--format','{{json .}}',...c.context().states.map(s=>s.Id)]).trim().split(/\r?\n/).map(l=>{const s=JSON.parse(l);return {service:s.Name,cpu:s.CPUPerc,memory:s.MemUsage};});
 const result={checkedAt:new Date().toISOString(),status:measurements.every(m=>m.errors===0)?'passed':'failed',measurements,containers:stats,scope:'Small local baseline; not capacity certification',cacheDecision:'Keep uncached public catalog until repeatable latency/DB-load evidence justifies staleness and invalidation complexity.'};
 fs.writeFileSync(path.join(c.root,'docs/docker/opt4-opt5-observation.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result,null,2));if(result.status==='failed')process.exitCode=1;
})().catch(e=>{console.error(e.message.split('\n')[0]);process.exitCode=1;}).finally(()=>clearInterval(keepAlive));
