// Synthetic one-pixel upload through Gateway. Remove only the asset created here.
const fs=require('fs'),path=require('path'),zlib=require('zlib');
const root=path.resolve(__dirname,'../..'),assert=require('assert/strict'),keepAlive=setInterval(()=>{},1000),checks=[];
const request=(url,opts={})=>fetch(url,{...opts,signal:AbortSignal.timeout(30000)});
const base='http://127.0.0.1:18080/api/v1/academic/media';let uploaded,adminToken;
function chunk(type,data){const t=Buffer.from(type),length=Buffer.alloc(4);length.writeUInt32BE(data.length);let crc=0xffffffff;for(const byte of Buffer.concat([t,data])){crc^=byte;for(let i=0;i<8;i++)crc=(crc>>>1)^((crc&1)?0xedb88320:0);}const tail=Buffer.alloc(4);tail.writeUInt32BE((crc^0xffffffff)>>>0);return Buffer.concat([length,t,data,tail]);}
const ihdr=Buffer.alloc(13);ihdr.writeUInt32BE(1);ihdr.writeUInt32BE(1,4);ihdr[8]=8;ihdr[9]=2;
const png=Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',ihdr),chunk('IDAT',zlib.deflateSync(Buffer.from([0,0,128,255]))),chunk('IEND',Buffer.alloc(0))]);
async function login(username){const r=await request('http://127.0.0.1:18080/api/v1/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username,password:'123456'})});assert.equal(r.status,200);return (await r.json()).token;}
(async()=>{try{
 assert.equal((await request(base+'/config')).status,401);checks.push('Anonymous access denied');
 const staff=await login('staff1');assert.equal((await request(base+'/config',{headers:{Authorization:'Bearer '+staff}})).status,403);checks.push('Staff access denied');
 const token=await login('admin1'),headers={Authorization:'Bearer '+token};adminToken=token;const cfg=await request(base+'/config',{headers});assert.equal(cfg.status,200);assert.equal((await cfg.json()).enabled,true);checks.push('Admin configuration ready; no credentials returned');
 const bad=new FormData();bad.set('file',new Blob(['not an image'],{type:'image/png'}),'bad.png');bad.set('category','courses');bad.set('branchId','1');assert.equal((await request(base+'/images',{method:'POST',headers,body:bad})).status,400);checks.push('Fake PNG rejected');
 const foreign=new FormData();foreign.set('file',new Blob([png]),'probe.png');foreign.set('category','courses');foreign.set('branchId','3');assert.equal((await request(base+'/images',{method:'POST',headers,body:foreign})).status,404);checks.push('Unassigned branch rejected');
 const form=new FormData();form.set('file',new Blob([png],{type:'image/png'}),'probe.png');form.set('category','courses');form.set('branchId','1');const r=await request(base+'/images',{method:'POST',headers,body:form});assert.equal(r.status,200,'Cloudinary upload returned HTTP '+r.status);uploaded=await r.json();assert.ok(uploaded.publicId.startsWith('lc_'));assert.equal(uploaded.width,1);assert.equal(uploaded.height,1);checks.push('Synthetic PNG uploaded through authenticated API');
 const gallery=await request(base+'/images?category=courses&branchId=1',{headers});assert.equal(gallery.status,200,'Image library API failed');const galleryRows=await gallery.json();assert.ok(galleryRows.some(row=>row.asset.id===uploaded.id));checks.push('Image library returns the uploaded asset');
 assert.equal((await request(uploaded.url)).status,200);checks.push('Cloudinary public delivery works');
 }finally{if(uploaded){const r=await request(base+'/images/'+uploaded.id,{method:'DELETE',headers:{Authorization:'Bearer '+adminToken}});assert.equal(r.status,204);checks.push('Synthetic cloud asset deleted after test');}}
 fs.writeFileSync(path.join(root,'docs/docker/cloudinary-validation.json'),JSON.stringify({checkedAt:new Date().toISOString(),status:'passed',checks,syntheticAssetDeleted:true},null,2));console.log(JSON.stringify({status:'passed',checks:checks.length}));
})().catch(e=>{fs.writeFileSync(path.join(root,'docs/docker/cloudinary-validation.json'),JSON.stringify({checkedAt:new Date().toISOString(),status:'failed',checks,error:e.message,syntheticAssetCreated:!!uploaded},null,2));console.error(e.message);process.exitCode=1;}).finally(()=>clearInterval(keepAlive));
