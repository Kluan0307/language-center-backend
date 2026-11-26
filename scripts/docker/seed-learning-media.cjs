// Explicit demo-data import through existing authenticated APIs; never alters schema.
// Run after taking a Docker backup. Progress is persisted after every mutation.
const c=require('./data-common.cjs');
const {fs,path,assert,root}=c,crypto=require('node:crypto');
const dir=path.join(root,'storage/sample-learning-media');fs.mkdirSync(dir,{recursive:true});
const stateFile=path.join(dir,'import.json');
const state=fs.existsSync(stateFile)?JSON.parse(fs.readFileSync(stateFile,'utf8')):{backupId:'ops-1795598691028-e5a7c7',images:{},publications:{},documents:{}};
const save=()=>fs.writeFileSync(stateFile,JSON.stringify(state,null,2));
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
const photos=[
 {key:'pencils',author:'Josephina Kolpachnikof',page:'https://unsplash.com/photos/a-group-of-colored-pencils-sitting-on-top-of-each-other-AWxDuxOIOkM',cdn:'1646318591979-1f5f1ffec11c'},
 {key:'study',author:'Clay Banks',page:'https://unsplash.com/photos/open-notebook-with-pen-and-pencils-on-desk-n9AaeihA9HI',cdn:'1761322572550-967ea8c0bfd9'},
 {key:'notebook',author:'Kelly Sikkema',page:'https://unsplash.com/photos/an-open-notebook-and-pens-on-a-desk-hBdaqrr5Z3k',cdn:'1743385779312-73ea241025d8'},
 {key:'planner',author:'My Profit Tutor',page:'https://unsplash.com/photos/a-notebook-with-a-pen-on-top-of-it-v30bSAWzp4I',cdn:'1692158962119-8103c7d78c86'}
];
// All passages, prompts and answers below are original demo teaching material.
const units=[
 {key:'mg-colours',course:17,title:'Mẫu giáo - Màu sắc và số đếm',level:'Pre-literacy / preschool',time:'15 minutes with an adult',heading:'Colours and numbers',sections:[
 ['Say and point','Adult: point to a real red, blue and yellow object. Say each colour slowly. Child: point and repeat. Repeat with a pencil, a book and a bag. Do not require the child to read.'],
 ['Listen and draw','Adult says: Draw one red ball. Draw two blue stars. Draw three yellow circles. Pause after each instruction and help with counting. The adult may draw outlines for the child to colour.'],
 ['Move and speak','Put three safe classroom objects on a table. Say: Show me one pencil. Show me two books. Then ask: What colour is it? Model: It is blue. Accept a one-word colour answer.'],
 ['Home practice','For five minutes, find three colours at home together. Ask permission before touching objects. Praise pointing, listening and speaking; do not assign a numeric test score.']],answers:[['Adult guide','Drawing: 1 red ball, 2 blue stars, 3 yellow circles. Object colours depend on the actual objects. Support the child rather than correcting every sound.'],['Success checklist','The child can point to at least two colours and count up to three objects with help. Repeat in a later lesson if needed.']]},
 {key:'primary-bag',course:19,title:'Tiểu học - Đồ dùng học tập',level:'Primary beginner',time:'25 minutes',heading:'My school bag',sections:[
 ['Word bank','book / pen / pencil / ruler / bag. Say each word and draw a small picture beside it.'],
 ['Read','My name is Mai. This is my blue bag. I have two books, one ruler and three pencils. My friend Ben has a green bag and one red pen.'],
 ['Check understanding','1. What colour is Mai\'s bag? 2. How many pencils does Mai have? 3. Who has a red pen? 4. Is Ben\'s bag blue?'],
 ['Complete and speak','Complete: I have ___ book. I have ___ pencils. Show your own bag to a partner. Ask: What is this? Answer: It is a ___. Use only objects that belong to you.'],
 ['Write','Draw your bag. Write three sentences: My bag is ___. I have ___. I have ___.']],answers:[['Reading answers','1. Blue. 2. Three. 3. Ben. 4. No, it is green.'],['Grammar and writing','Use a/an for one item, or a number: I have a book / one book. Use a number with plural pencils. Writing answers vary; check the noun and number agree.']]},
 {key:'a1-routine',course:21,title:'A1 - Giới thiệu và thói quen',level:'A1',time:'30 minutes',heading:'A new classmate',sections:[
 ['Read','Hello! My name is Linh. I am nineteen and I live in Da Nang. I study English on Monday and Wednesday evenings. My class starts at six and finishes at seven thirty. On Saturday, I work in a small cafe. I like music and swimming.'],
 ['Find the information','1. How old is Linh? 2. Where does she live? 3. When does she study English? 4. What time does class finish? 5. Where does she work on Saturday?'],
 ['Choose the word','1. Linh (live / lives) in Da Nang. 2. I (am / is) a student. 3. Her class (start / starts) at six. 4. We (study / studies) English.'],
 ['Pair practice','Ask a partner: What is your name? Where do you live? When do you study? What do you like? Use fictional information if you prefer.'],
 ['Write','Write 40-60 words about your weekly routine. Include two days, two times and one hobby.']],answers:[['Reading answers','1. Nineteen. 2. Da Nang. 3. Monday and Wednesday evenings. 4. At seven thirty. 5. In a small cafe.'],['Grammar answers','1. lives 2. am 3. starts 4. study.'],['Writing check','Include a clear subject in each sentence. Use -s for he/she/it in the present simple. Check days start with a capital letter.']]},
 {key:'communication-plans',course:23,title:'Giao tiếp - Hẹn lịch và xác nhận',level:'A1-A2 communication',time:'30 minutes',heading:'Making a study plan',sections:[
 ['Useful language','Are you free on Thursday? / Could we meet at six? / Sorry, I cannot make it. / How about Friday? / That works for me. / Let me confirm the time.'],
 ['Complete the conversation','A: Are you free on Thursday at six? B: Sorry, I ___ make it. A: How ___ Friday at five? B: That works ___ me. A: Great. Let me ___: Friday at five in the library.'],
 ['Role play A','You want a 30-minute study meeting. You are free Tuesday 17:00-18:00 and Friday 16:00-17:00. Ask your partner and agree on one time.'],
 ['Role play B','You are busy on Tuesday. On Friday you are free 16:30-18:00. Suggest a suitable 30-minute meeting and repeat the agreed place and time.'],
 ['Follow-up message','Write a 30-40 word message confirming the day, start time, end time, place and purpose. Read it aloud clearly.']],answers:[['Gap answers','cannot / about / for / confirm.'],['Possible agreement','Friday 16:30-17:00 in the library. Both partners are available during that full 30-minute period.'],['Self-check','Did both speakers agree? Did you confirm day, time and place? A different time is valid only if it fits both availability windows.']]},
 {key:'ielts-trends',course:26,title:'IELTS 5.5 - Mô tả xu hướng',level:'IELTS preparation / introductory writing',time:'35 minutes',heading:'Describing a small data table',sections:[
 ['Original practice data','A fictional language club recorded weekly visits: January 40; February 55; March 50; April 70. These are made-up figures for this worksheet, not real centre statistics.'],
 ['Understand the data','1. Which month has the highest figure? 2. Which month has the lowest? 3. What is the change from January to April? 4. Which interval shows a fall?'],
 ['Language practice','Complete: Visits rose ___ 40 ___ 55 between January and February. They fell ___ five in March, then increased ___ 70 in April.'],
 ['Plan your response','Group the overall upward trend separately from the small March decline. Compare the beginning and end. Describe only what the table shows; do not invent reasons.'],
 ['Writing task','Write at least 150 words describing the data. Include an introduction, an overview and selected comparisons. Use your own wording and check every number.']],answers:[['Data answers','1. April (70). 2. January (40). 3. An increase of 30 visits. 4. February to March (55 to 50).'],['Language answers','from / to / by / to. A rise of 30 from an initial 40 is 75 percent; do not confuse 30 visits with 30 percent.'],['Review checklist','Check units, past tense, comparisons and overview. This is original classroom practice, not an official IELTS test, band prediction or endorsed preparation product. Official resources: https://ielts.org/take-a-test/preparation-resources']]},
 {key:'toeic-email',course:30,title:'TOEIC 450 - Đọc email công việc',level:'TOEIC preparation / elementary workplace English',time:'25 minutes',heading:'An office training email',sections:[
 ['Read the original email','To: Sales team. Subject: Friday training. Our customer-service workshop is on Friday, 18 December, from 9:00 to 10:30 in Room B. Please bring a notebook. The workshop is free for staff. Email Mina before Wednesday if you need to join online. The office will send the meeting link on Thursday.'],
 ['Choose the answer','1. What is the email mainly about? A. A product price B. A training session C. A job application. 2. Where is the workshop? A. Room B B. Room C C. The cafe. 3. When is the online link sent? A. Wednesday B. Thursday C. Friday.'],
 ['Find details','4. How long is the workshop? 5. What should participants bring? 6. Who should staff contact about online attendance?'],
 ['Workplace vocabulary','Match: participant / workshop / confirm. Meanings: a training event / say that details are correct / a person taking part.'],
 ['Reply','Write a short polite email requesting online attendance. Include the workshop date and a clear request. Do not include any real contact details.']],answers:[['Answers','1. B 2. A 3. B 4. 90 minutes 5. A notebook 6. Mina. Participant = a person taking part; workshop = a training event; confirm = say that details are correct.'],['Example reply','Hello Mina, I would like to join the workshop on 18 December online. Could you please add me to the online attendance list? Thank you.'],['Resource note','Original classroom material, not official TOEIC questions or a score predictor. Official preparation: https://www.ets.org/toeic/test-takers/prepare.html']]},
 {key:'b1-study-options',course:22,title:'B1 - So sánh cách học',level:'B1',time:'35 minutes',heading:'Choosing how to study',sections:[
 ['Read','An wants to improve her speaking. Her evening class offers live pair work and immediate teacher feedback, but she needs thirty minutes to travel there. An online self-study course is cheaper and fits her work schedule. However, she often postpones lessons when nobody is waiting for her. She decides to attend class twice a week and use short online activities on other days.'],
 ['Comprehension','1. Give two advantages of the evening class. 2. What is the travel disadvantage? 3. Why might self-study be difficult for An? 4. What combined plan does she choose?'],
 ['Link ideas','Combine each pair with although, because or however: The online course is flexible. An needs live speaking practice. / She chooses classes. She values teacher feedback.'],
 ['Discussion','Which method suits a person with changing work hours? Which suits a person who needs accountability? Give a reason and ask your partner one follow-up question.'],
 ['Write','Write 100-120 words recommending a realistic weekly study plan. Mention one advantage and one limitation of your choice.']],answers:[['Reading answers','1. Live pair work and immediate feedback. 2. A thirty-minute journey. 3. She postpones lessons without external accountability. 4. Class twice weekly plus short online activities on other days.'],['Possible linking','Although the online course is flexible, An needs live speaking practice. She chooses classes because she values teacher feedback.'],['Review','Different recommendations are valid if supported by the learner needs. Check sentence links, realistic times and one clear limitation.']]}
];
// Small dependency-free PDF writer. Text is deliberately ASCII English; no broken font fallback.
function pdf(unit){
 const esc=s=>s.replace(/\\/g,'\\\\').replace(/\(/g,'\\(').replace(/\)/g,'\\)');
 const wrap=s=>{const out=[];let line='';for(const word of s.split(/\s+/)){if((line+' '+word).trim().length>86){out.push(line);line=word;}else line=(line+' '+word).trim();}if(line)out.push(line);return out;};
 const pages=[unit.sections,unit.answers],objects=[];
 objects.push('<< /Type /Catalog /Pages 2 0 R >>','', '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');
 const kids=[];
 pages.forEach((sections,index)=>{const page=objects.length+1,content=page+1;kids.push(page+' 0 R');
 let y=790,stream='0.08 0.25 0.55 rg\n';
 const text=(s,size=11)=>{stream+='BT /F1 '+size+' Tf 48 '+y+' Td ('+esc(s)+') Tj ET\n';y-=size+7;};
 text('LANGUAGE CENTER | SAMPLE LEARNING MATERIAL',12);text(unit.heading+(index?' - Self-check':''),19);text(unit.level+' | '+unit.time,10);y-=8;stream+='0.12 0.16 0.22 rg\n';
 for(const [heading,body] of sections){text(heading,12);for(const line of wrap(body))text(line,10);y-=12;}
 assert.ok(y>70,'PDF content exceeds page: '+unit.key);
 stream+='BT /F1 8 Tf 48 42 Td (Original demo worksheet | Not an official examination | '+(index+1)+' / 2) Tj ET\n';
 objects.push('<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents '+content+' 0 R >>','<< /Length '+Buffer.byteLength(stream)+' >>\nstream\n'+stream+'endstream');
 });
 objects[1]='<< /Type /Pages /Kids ['+kids.join(' ')+'] /Count 2 >>';
 let result='%PDF-1.4\n',offsets=[0];objects.forEach((o,i)=>{offsets.push(Buffer.byteLength(result));result+=(i+1)+' 0 obj\n'+o+'\nendobj\n';});
 const xref=Buffer.byteLength(result);result+='xref\n0 '+offsets.length+'\n0000000000 65535 f \n'+offsets.slice(1).map(n=>String(n).padStart(10,'0')+' 00000 n \n').join('')+'trailer\n<< /Size '+offsets.length+' /Root 1 0 R >>\nstartxref\n'+xref+'\n%%EOF\n';return Buffer.from(result);
}
let token;
async function api(route,options={}){const r=await fetch('http://127.0.0.1:18080/api/v1/'+route,{...options,headers:{...(token?{Authorization:'Bearer '+token}:{}),...options.headers},signal:AbortSignal.timeout(60000)});if(!r.ok)throw Error(route+' HTTP '+r.status+' '+(await r.text()).slice(0,350));return r;}
const json=(method,value)=>({method,headers:{'Content-Type':'application/json'},body:JSON.stringify(value)});
async function main(){
 const docsDir=path.join(dir,'documents');fs.mkdirSync(docsDir,{recursive:true});for(const u of units)fs.writeFileSync(path.join(docsDir,u.key+'.pdf'),pdf(u));
 fs.writeFileSync(path.join(dir,'sources.json'),JSON.stringify({license:'https://unsplash.com/license',photos:photos.map(p=>({...p,download:'https://images.unsplash.com/photo-'+p.cdn+'?fm=jpg&fit=crop&w=1400&q=82',sha256:hash(fs.readFileSync(path.join(dir,'sources',p.key+'.jpg')))})),documents:'Original Language Center sample worksheets; external links are further reading only, not copied exercises.'},null,2));
 if(process.argv.includes('--prepare-only'))return;
 assert.ok(fs.existsSync(path.join(root,'storage/docker-backups',state.backupId,'backup.json')),'Backup must exist before import');
 token=(await (await api('auth/login',json('POST',{username:process.env.LC_SEED_USERNAME||'superadmin',password:process.env.LC_SEED_PASSWORD||'123456'}))).json()).token;
 for(const category of ['courses','banners'])for(const p of photos){const key=category+'/'+p.key;if(state.images[key])continue;const f=new FormData();f.set('category',category);f.set('file',new Blob([fs.readFileSync(path.join(dir,'sources',p.key+'.jpg'))],{type:'image/jpeg'}),p.key+'.jpg');state.images[key]=await (await api('academic/media/images',{method:'POST',body:f})).json();save();console.log('Uploaded '+key);}
 const fields=['sourceId','version','slug','state','summary','audience','outcomes','topics','imageUrl','openingHours','mapUrl','programId','showTuition'];
 for(const kind of ['PROGRAM','COURSE']){const data=await (await api('academic/website/publications/'+kind+'?page=0')).json();const rows=Array.isArray(data)?data:(data.items||data.content);assert.ok(rows,'Unexpected CMS list shape');for(const row of rows){const key=kind+'/'+row.sourceId;if(state.publications[key]?.status==='done')continue;
 const source=Number(row.sourceId);if(kind==='COURSE'&&(source<17||source>36))continue;
 const photo=kind==='PROGRAM'?({1:'notebook',2:'study',3:'planner',4:'study'}[source]):source<=20?'pencils':source<=22?'notebook':source>=29&&source<=32?'planner':'study';assert.ok(photo,'Unknown program');
 // Reconcile a successful save if the process stopped before persisting its response.
 if(state.publications[key]?.status==='pending'&&row.imageUrl===state.images[(kind==='PROGRAM'?'banners':'courses')+'/'+photo].url){state.publications[key].after=row;state.publications[key].status='done';save();continue;}
 // Preserve manually curated images. No replacement of existing photographs.
 if(row.imageUrl)continue;
 state.publications[key]={before:row,status:'pending'};save();
 const body=Object.fromEntries(fields.map(k=>[k,row[k]??null]));body.showTuition=!!row.showTuition;body.imageUrl=state.images[(kind==='PROGRAM'?'banners':'courses')+'/'+photo].url;
 body.summary=((row.summary||'').trim()+'\nẢnh minh họa học tập.').trim();
 const result=await (await api('academic/website/publications/'+kind,json('PUT',body))).json();state.publications[key]={before:row,after:result,status:'done'};save();console.log('Updated '+key);
 }}
 for(let branch=1;branch<=4;branch++)for(const u of units){const key=branch+'/'+u.key;let d=state.documents[key];if(!d){
 // Recover by unique seed marker after interruption between API response and local save.
 const listed=await (await api('teaching/documents?branchId='+branch+'&courseId='+u.course+'&size=100')).json();const marker='[LC-SAMPLE:'+u.key+']';d=(listed.items||listed.content||[]).find(x=>x.description?.includes(marker));
 if(!d){const form=new FormData();form.set('title','[Tài liệu mẫu] '+u.title);form.set('description',marker+' Bài tập tự biên soạn, gồm hướng dẫn và đáp án tự kiểm tra; không phải đề thi chính thức.');form.set('branchId',String(branch));form.set('courseId',String(u.course));form.set('file',new Blob([pdf(u)],{type:'application/pdf'}),u.key+'.pdf');d=await (await api('teaching/documents',{method:'POST',body:form})).json();}state.documents[key]=d;save();}
 d=await (await api('teaching/documents/'+d.id)).json();if(d.status==='PENDING'){d=await (await api('teaching/documents/'+d.id+'/review',json('PATCH',{status:'APPROVED',version:d.version,reason:'Duyệt bộ bài tập mẫu tự biên soạn, đúng chương trình và phạm vi chi nhánh.'}))).json();state.documents[key]=d;save();}
 assert.equal(d.status,'APPROVED');if(!d.sharedToStudents){d=await (await api('teaching/documents/'+d.id+'/student-sharing',json('PATCH',{version:d.version,sharedToStudents:true,reason:'Chia sẻ bài tập mẫu phù hợp khóa học cho học viên trong chi nhánh.'}))).json();state.documents[key]=d;save();}
 const downloaded=Buffer.from(await (await api('teaching/documents/'+d.id+'/download')).arrayBuffer());assert.equal(hash(downloaded),hash(pdf(u)));console.log('Verified document '+key+' #'+d.id);
 }
 for(const asset of Object.values(state.images)){const r=await fetch(asset.url,{signal:AbortSignal.timeout(30000)});assert.equal(r.status,200);assert.equal(hash(Buffer.from(await r.arrayBuffer())),hash(fs.readFileSync(path.join(dir,'sources',Object.entries(state.images).find(([,v])=>v.id===asset.id)[0].split('/')[1]+'.jpg'))));}
 const report={checkedAt:new Date().toISOString(),status:'passed',target:'Docker database; Windows MySQL untouched',backupId:state.backupId,sourcePhotos:photos.length,cloudAssets:Object.keys(state.images).length,updatedPublications:Object.values(state.publications).filter(x=>x.status==='done').length,approvedSharedDocuments:Object.keys(state.documents).length,templates:units.map(u=>({key:u.key,courseId:u.course,title:u.title})),photos,checks:['Authenticated API import','Existing image references preserved','Optimistic CMS version checks','Documents approved before sharing','All 28 document downloads match original SHA-256','Cloud images downloadable and match source SHA-256']};
 fs.writeFileSync(path.join(root,'docs/docker/sample-learning-media.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report));
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
