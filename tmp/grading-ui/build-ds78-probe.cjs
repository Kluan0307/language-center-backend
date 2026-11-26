const fs=require('fs'),path=require('path');const root=path.resolve(__dirname,'../..');let s=fs.readFileSync(path.join(root,'scripts/docker/verify-grading-outcome-live.cjs'),'utf8');
s=s.replaceAll('ds456','ds78').replaceAll('19260','19262').replace('grading-outcome-acceptance-','grading-transition-acceptance-');
s=s.replace("let studentToken,otherStudentToken,academicVersion=1;",`let studentToken,otherStudentToken,academicVersion=1;
function sourceProof(classId){const row=mysql('SELECT token,source_revision FROM class_result_barriers WHERE class_id='+Number(classId),db).trim().split('\\t');return {token:row[0]||'pending',sourceRevision:Number(row[1]||0)+academicVersion-1};}`);
s=s.replace("{token:'synthetic-proof',sourceRevision:academicVersion}","sourceProof(classId)").replace("{classId:17,token:'synthetic-proof',sourceRevision:academicVersion}","{classId:17,...sourceProof(17)}");
s=s.replace('// Select the clean activity through Ant Design and exercise the real entry form.',`// Select the grading entry tab before exercising Ant Design.
 await evaluate('[...document.querySelectorAll(".ant-tabs-tab")].find(e=>e.textContent.trim()==="Nhập kết quả").click()');
 // Select the clean activity through Ant Design and exercise the real entry form.`);
s=s.replace("async function uiAction(label){",`async function uiAction(label){await evaluate('[...document.querySelectorAll(".ant-tabs-tab")].find(e=>e.textContent.trim()==="Tổng hợp & công bố").click()');`);
s=s.replace('document.body.innerText.includes("Quy chế của lớp")&&document.body.innerText.includes("Điểm nội bộ")','document.body.innerText.includes("Nhập kết quả")&&document.body.innerText.includes("Bài đánh giá")');
const anchor=" await workflow(admin,'RETRACT');assert.equal((await studentView(studentToken,1001)).result,null);";
if(!s.includes(anchor))throw Error('Probe insertion anchor');
s=s.replace(anchor,`
 const attempt=visible.result.components.find(c=>c.code==='FINAL').assessments.find(a=>a.rootId===retake.retakeOf);assert.equal(attempt.history.length,2);assert.equal(attempt.history.find(h=>h.selected).score,9);assert.equal(attempt.history.find(h=>!h.selected).score,0);pass('Published snapshot preserves selected and previous retake scores');
 async function personalCsv(token,id,expected=200){const r=await request(base+'/api/v1/teaching/student/me/enrollments/'+id+'/grading-result/export',{headers:{Authorization:'Bearer '+token}});assert.equal(r.status,expected);return r.status===200?Buffer.from(await r.arrayBuffer()):null;}
 const csv=await personalCsv(studentToken,1001);assert.equal(csv[0],239);assert.ok(csv.toString('utf8').includes('Demo learner A'));assert.ok(!csv.toString('utf8').includes('Demo learner B'));await personalCsv(studentToken,1002,404);pass('Personal UTF-8 CSV contains own immutable result and denies another enrollment');
 const cp=await api(admin,'/classes/17/completion/preview','POST');assert.deepEqual(cp.blockers,[]);const command={requestId:crypto.randomUUID(),fingerprint:cp.fingerprint,reason:'Synthetic official completion, no Academic mutation'};
 const confirmed=await api(admin,'/classes/17/completion/confirm','POST',command);assert.equal(confirmed.results.length,2);assert.ok(confirmed.results.every(r=>r.state==='CONFIRMED'&&r.gradingReleaseId));assert.equal((await api(admin,'/classes/17/completion/confirm','POST',command)).results.length,2);assert.equal(mysql('SELECT confirmed_count FROM class_result_barriers WHERE class_id=17',db).trim(),'2');pass('MySQL completion batch creates current source-bound proofs and idempotent retry');
 await api(teacher,'/classes/17/activities','POST',{name:'Blocked final mutation',date,components:['FINAL']},409);
`+anchor+`
 assert.equal(mysql('SELECT confirmed_count FROM class_result_barriers WHERE class_id=17',db).trim(),'0');assert.equal(mysql("SELECT COUNT(*) FROM final_results WHERE class_id=17 AND state='CONFIRMED'",db).trim(),'0');await personalCsv(studentToken,1001,404);pass('Retraction invalidates official proofs and personal CSV');
`);
const beforeShots=" const shots=path.join(root,'docs/grading/screenshots');";
s=s.replace(beforeShots,`
 async function teaching(token,route,method,body,expected=200){const r=await request(base+'/api/v1/teaching'+route,{method,headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:JSON.stringify(body)});const txt=await r.text();assert.equal(r.status,expected,route+' '+txt.slice(0,200));return txt?JSON.parse(txt):null;}
 const oldExam=await teaching(superToken,'/exams','POST',{classId:23,name:'Legacy exact-scale final',examType:'FINAL',examDate:date,weight:40,maxScore:10});
 await teaching(teacher,'/grades','POST',{examId:oldExam.id,studentId:401,score:7,comment:'PRIVATE_DO_NOT_COPY',studentComment:'Public feedback'});
 const plan={templateId:'INTERNAL-v1',mappings:[{examId:oldExam.id,component:'FINAL'}]};
 const lp=await api(admin,'/classes/23/legacy/preview','POST',plan);assert.deepEqual(lp.blockers,[]);assert.equal(lp.grades,1);assert.equal((await api(admin,'/classes/23/binding')),null);pass('Legacy MySQL preview checks full mapping without creating binding');
 const lc={requestId:crypto.randomUUID(),fingerprint:lp.fingerprint,plan,reason:'Synthetic reviewed exact-scale mapping'};await api(admin,'/classes/23/legacy/apply','POST',lc);await api(admin,'/classes/23/legacy/apply','POST',lc);
 const imported=await api(admin,'/classes/23/activities');assert.equal(imported.length,1);const copy=await api(teacher,'/classes/23/activities/'+imported[0].id+'/sheet');assert.equal(copy.records[0].score,7);assert.equal(copy.records[0].comment,'Public feedback');assert.equal(mysql('SELECT COUNT(*) FROM grades WHERE exam_id='+oldExam.id,db).trim(),'1');assert.equal((await api(admin,'/classes/23/preview')).workflow.state,'DRAFT');pass('MySQL migration copies exact public scores once and retains originals unpublished');
 await teaching(teacher,'/grades','POST',{examId:oldExam.id,studentId:401,score:8,reason:'Denied legacy change'},409);
 const wrong=await teaching(superToken,'/exams','POST',{classId:24,name:'Legacy whole IELTS total',examType:'FINAL',examDate:date,weight:100,maxScore:9});const blocked=await api(admin,'/classes/24/legacy/preview','POST',{templateId:'IELTS-v1',mappings:[{examId:wrong.id,component:'LISTENING'}]});assert.ok(blocked.blockers.length);pass('Legacy exam total cannot be silently converted to individual IELTS skills');
`+beforeShots);
s=s.replaceAll("'/grades','POST'","'/grades','PUT'");
s=s.replace('assert.equal(attempt.history.find(h=>!h.selected).score,0)','assert.equal(attempt.history.find(h=>!h.selected).score,7)');
s=s.replace("throw Error(r.exceptionDetails.text)","throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text)");
const historyAnchor="pass('Published snapshot preserves selected and previous retake scores');";
s=s.replace(historyAnchor,historyAnchor+`
 await wait('[...document.querySelectorAll("button")].some(b=>b.textContent.trim()==="Xem cách tính")');await evaluate('[...document.querySelectorAll("button")].find(b=>b.textContent.trim()==="Xem cách tính").click()');await wait('!!document.querySelector(".ant-drawer-open")');await evaluate('[...document.querySelectorAll(".ant-drawer-open .ant-table-row-expand-icon")].forEach(b=>b.click())');await wait('document.querySelector(".ant-drawer-open").innerText.includes("Synthetic retake")');assert.ok(await evaluate('document.querySelector(".ant-drawer-open").innerText.includes("Đang dùng")'));await send('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await send('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await wait('!document.querySelector(".ant-drawer-open")');pass('Real result drawer displays retake history and selected attempt');
`);
fs.writeFileSync(path.join(root,'scripts/docker/verify-grading-transition-live.cjs'),s);
console.log('DS78 isolated MySQL / browser acceptance prepared.');
