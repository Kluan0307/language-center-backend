// Exercises restart/image update on the authorized local Docker stack.
// No SQL writes are issued; external-provider flags remain untouched.
const c=require('./data-common.cjs'),ops=require('./operations.cjs');
const {fs,path,root,assert,compose,dockerSnapshot,context}=c,checks=[],keepAlive=setInterval(()=>{},1000);
const pass=name=>{checks.push(name);console.log(name);};
(async()=>{
 const before=dockerSnapshot(),ctx=context(),documents=ops.archiveManifest(ops.getArchive('language-center_documents'));
 assert.equal(ctx.states.length,9);assert.ok(ctx.states.every(s=>s.State.Health?.Status==='healthy'));
 const volumes=JSON.parse(c.docker(['volume','inspect','language-center_mysql_data','language-center_documents'])).map(v=>({name:v.Name,createdAt:v.CreatedAt}));
 const updated=ops.update(['web-admin','website']);assert.equal(updated.status,'passed');pass('Both web images build and deploy from current Compose configuration');
 assert.deepEqual(dockerSnapshot(),before);assert.deepEqual(ops.archiveManifest(ops.getArchive('language-center_documents')),documents);pass('Image update preserves all rows and documents');
 const rolledBack=ops.rollback(updated.backupId);assert.equal(rolledBack.status,'passed');pass('Previous web image tags are retained and rollback starts healthy containers');
 console.log('Testing full down/up; named volumes are retained.');
 compose(['down','--remove-orphans']);compose(['up','-d','--no-build','--wait','--wait-timeout','240']);
 assert.deepEqual(JSON.parse(c.docker(['volume','inspect','language-center_mysql_data','language-center_documents'])).map(v=>({name:v.Name,createdAt:v.CreatedAt})),volumes);pass('Full Compose down/up retains the original DB and document volumes');
 assert.deepEqual(dockerSnapshot(),before);assert.deepEqual(ops.archiveManifest(ops.getArchive('language-center_documents')),documents);pass('All row and file hashes match after recreation of all nine containers');
 const state=await ops.status();assert.equal(state.status,'passed');assert.equal(state.services.length,9);pass('Nine recreated containers healthy; Gateway ready and data available');
 for(const base of [state.urls['web-admin'],state.urls.website]){const r=await fetch(base,{signal:AbortSignal.timeout(15000)});assert.equal(r.status,200);}pass('Both web origins respond after full restart');
 const r=await fetch(state.urls.website+'/api/catalog?kind=COURSE',{signal:AbortSignal.timeout(15000)});assert.equal(r.status,200);assert.ok((await r.json()).items.length>0);pass('Next server resolves recreated Gateway and returns published courses');
 const result={checkedAt:new Date().toISOString(),status:'passed',checks,tables:before.tables.length,rows:before.rowCount,backupId:updated.backupId,fullStackRestart:true,imageUpdateTested:['web-admin','website'],imageRollbackTested:true,allDataPreserved:true,sourceDatabaseUsed:false};
 fs.writeFileSync(path.join(root,'docs/docker/dck6-lifecycle-validation.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({status:'passed',checks:checks.length,backupId:updated.backupId}));
})().catch(e=>{console.error(e.message.split('\n').slice(0,2).join('\n'));process.exitCode=1;}).finally(()=>clearInterval(keepAlive));
