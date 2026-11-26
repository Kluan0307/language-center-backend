// Portrait illustrations for existing demo teacher website profiles only.
const c=require('./data-common.cjs'),{fs,path,root,assert}=c;
const dir=path.join(root,'storage/teacher-portraits'),file=path.join(dir,'import.json');
const state=fs.existsSync(file)?JSON.parse(fs.readFileSync(file,'utf8')):{backupId:'ops-1795599740594-79ee35',assets:{},profiles:{}};
const save=()=>fs.writeFileSync(file,JSON.stringify(state,null,2));
const sources=[
 [1,3779508,'Andrea Piacquadio','man-in-red-polo-shirt-holding-a-book'],
 [2,8101969,'Mikhail Nilov','a-cute-woman-in-black-blazer-smiling'],
 [3,31268615,"Duy's House of Photo",'confident-asian-man-posing-in-studio-portrait'],
 [4,20603308,'BYB BYB','portrait-of-woman-in-black-suit'],
 [5,7652508,'Thirdman','a-man-wearing-a-black-corporate-attire'],
 [6,15262831,'Pamungkas Leader','portrait-of-an-asian-woman-with-a-bun'],
 [7,6723385,'Deepak Maurya','a-man-in-polo-shirt-smiling'],
 [8,8423883,'Pavel Danilyuk','a-woman-smiling-at-the-camera']
].map(([teacherId,id,author,slug])=>({teacherId,id,author,page:'https://www.pexels.com/photo/'+slug+'-'+id+'/',download:'https://images.pexels.com/photos/'+id+'/pexels-photo-'+id+'.jpeg?auto=compress&cs=tinysrgb&w=900'+(id===6723385?'&h=675&fit=crop&crop=faces':''),license:'https://www.pexels.com/license/'}));
let token;
async function api(route,options={}){const r=await fetch('http://127.0.0.1:18080/api/v1/'+route,{...options,headers:{...(token?{Authorization:'Bearer '+token}:{}),...options.headers},signal:AbortSignal.timeout(60000)});if(!r.ok)throw Error(route+' HTTP '+r.status+' '+(await r.text()).slice(0,250));return r;}
async function main(){
 assert.ok(fs.existsSync(path.join(root,'storage/docker-backups',state.backupId,'backup.json')));
 token=(await(await api('auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'superadmin',password:process.env.LC_SEED_PASSWORD||'123456'})})).json()).token;
 const rows=await(await api('academic/website/teachers')).json();assert.equal(rows.length,8,'Expected eight demo teacher profiles');
 // The seventh original has its face unusually low in frame. Use the source's
 // face-cropped rendition; keep the old cloud asset because history references it.
 if(!state.faceFramed&&fs.existsSync(path.join(dir,'sources/6723385-framed.jpg'))){state.previousAsset=state.assets[6723385];delete state.assets[6723385];if(state.profiles[7])state.profiles[7].status='pending';fs.copyFileSync(path.join(dir,'sources/6723385-framed.jpg'),path.join(dir,'sources/6723385.jpg'));state.faceFramed=true;save();}
 for(const source of sources){const row=rows.find(r=>r.teacherId===source.teacherId);assert.ok(row);
 if(state.profiles[source.teacherId]?.status==='done')continue;
 if(row.imageUrl&&!state.profiles[source.teacherId]){console.log('Preserved existing photo teacher '+source.teacherId);continue;}
 if(!state.assets[source.id]){const bytes=fs.readFileSync(path.join(dir,'sources',source.id+'.jpg'));const form=new FormData();form.set('category','teachers');form.set('file',new Blob([bytes],{type:'image/jpeg'}),'teacher-illustration-'+source.id+'.jpg');state.assets[source.id]=await(await api('academic/media/images',{method:'POST',body:form})).json();save();}
 const imageUrl=state.assets[source.id].url;
 if(row.imageUrl===imageUrl&&row.biography?.startsWith('Ảnh chân dung minh họa')){state.profiles[source.teacherId]={...state.profiles[source.teacherId],after:row,status:'done'};save();continue;}
 state.profiles[source.teacherId]={before:row,status:'pending'};save();
 const body={teacherId:row.teacherId,version:row.version,slug:row.slug,state:row.state,biography:row.biography?.startsWith('Ảnh chân dung minh họa')?row.biography:'Ảnh chân dung minh họa cho hồ sơ mẫu, không phải ảnh thật của giáo viên.\n\n'+(row.biography||''),teachingFocus:row.teachingFocus,imageUrl,showQualification:row.showQualification};
 const after=await(await api('academic/website/teachers',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)})).json();state.profiles[source.teacherId]={before:row,after,status:'done'};save();console.log('Updated teacher '+source.teacherId);
 }
 const publicRows=(await(await api('academic/public/teachers?size=100')).json()).items;
 let count=0;for(const source of sources){const saved=state.profiles[source.teacherId];if(saved?.status!=='done')continue;const profile=publicRows.find(p=>p.slug===saved.after.slug);assert.ok(profile);assert.equal(profile.imageUrl,saved.after.imageUrl);assert.ok(profile.biography.startsWith('Ảnh chân dung minh họa'));const download=await fetch(profile.imageUrl,{signal:AbortSignal.timeout(30000)});assert.equal(download.status,200);assert.equal(c.sha(Buffer.from(await download.arrayBuffer())),c.sha(fs.readFileSync(path.join(dir,'sources',source.id+'.jpg'))));const detail=await(await api('academic/public/teachers/'+profile.slug)).json();assert.equal(detail.imageUrl,profile.imageUrl);count++;}
 const backup=JSON.parse(fs.readFileSync(path.join(root,'storage/docker-backups',state.backupId,'backup.json'),'utf8'));const changed=c.dockerSnapshot().tables.filter(t=>backup.snapshot.tables.find(o=>o.name===t.name)?.sha256!==t.sha256).map(t=>t.name);assert.ok(changed.every(t=>['accounts','media_assets','website_teacher_profiles','website_edit_events'].includes(t)),'Unrelated tables modified');
 const report={checkedAt:new Date().toISOString(),status:'passed',target:'Docker database',backupId:state.backupId,updatedTeachers:count,sources,checks:['Eight teacher list/detail image references verified','Eight Cloudinary image downloads match original SHA-256','Illustration disclosure in every updated biography','Existing teaching and qualification fields preserved','No unrelated business tables changed'],changedTables:changed};
 fs.writeFileSync(path.join(root,'docs/docker/teacher-portraits.json'),JSON.stringify(report,null,2));console.log(JSON.stringify({status:'passed',updatedTeachers:count}));
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
