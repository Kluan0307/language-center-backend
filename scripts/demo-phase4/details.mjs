import assert from 'node:assert/strict';
import {api,ok,ready,suite,loadState,saveState,secrets,uuid} from '../acceptance/client.mjs';
const t=suite('g4-details'),s=loadState(),{sa,admin,staff,teacher,foreign,staffB}=secrets.accounts;
const keep=async(key,make)=>{if(!s[key]){s[key]=await make();saveState(s);}return s[key];};
const shift=(date,n)=>{const d=new Date(date+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+n);return d.toISOString().slice(0,10);};
try{
await ready();
const a1=await keep('a1',()=>ok('/academic/programs/'+s.program.id+'/levels',sa,'POST',{code:'A1',name:'A1',rank:0,active:true}));
const ielts=await keep('ielts',()=>ok('/academic/programs',sa,'POST',{code:'IELTS',name:'IELTS',active:true}));
const toeic=await keep('toeic',()=>ok('/academic/programs',sa,'POST',{code:'TOEIC',name:'TOEIC',active:true}));
for(const [key,p,code,name,rank]of [['ieltsBasic',ielts,'CB','Cơ bản',1],['ielts55',ielts,'5.5','Mục tiêu 5.5',2],['toeicBasic',toeic,'CB','Cơ bản',1],['toeic450',toeic,'450','Mục tiêu 450',2]])await keep(key,()=>ok('/academic/programs/'+p.id+'/levels',sa,'POST',{code,name,rank,active:true}));
const course2=await keep('course2',()=>ok('/academic/courses',admin,'POST',{code:'GT-A2',name:'Giao tiếp A2',expectedMinutes:720,tuitionFee:3000000,maxStudents:15,branchId:s.branchA.id}));
const course3=await keep('course3',()=>ok('/academic/courses',admin,'POST',{code:'IELTS-CB',name:'IELTS cơ bản',expectedMinutes:720,tuitionFee:4000000,maxStudents:15,branchId:s.branchA.id}));
const course4=await keep('course4',()=>ok('/academic/courses',sa,'POST',{code:'TOEIC-CB',name:'TOEIC cơ bản',expectedMinutes:720,tuitionFee:2500000,maxStudents:15,branchId:s.branchB.id}));
for(const [key,c,mode,p,level,accepted,actor]of [['policy2',course2,'REQUIRES_ASSESSMENT',s.program,s.a2,[a1.id],admin],['policy3',course3,'OPEN_ENTRY',ielts,s.ieltsBasic,[],admin],['policy4',course4,'OPEN_ENTRY',toeic,s.toeicBasic,[],sa]])await keep(key,()=>ok('/academic/courses/'+c.id+'/admission',actor,'PUT',{expectedRevision:0,mode,programId:p.id,outputLevelId:level.id,acceptedLevelIds:accepted,reason:'Xác nhận điều kiện tuyển sinh của khóa'}));
const room2=await keep('room2',()=>ok('/academic/rooms',admin,'POST',{code:'P2',name:'Phòng 2',capacity:20,branchId:s.branchA.id}));
const room3=await keep('room3',()=>ok('/academic/rooms',sa,'POST',{code:'P1',name:'Phòng 1',capacity:20,branchId:s.branchB.id}));
const weekday=new Date(s.today+'T12:00:00Z').getUTCDay();s.nextMonday=shift(s.today,((8-weekday)%7)||7);saveState(s);
for(const [key,c,b,teach,room,days,time,actor]of [['class2',course2,s.branchA,teacher,room2,['MONDAY','WEDNESDAY'],'18:00',admin],['class3',course3,s.branchA,teacher,s.room,['TUESDAY','THURSDAY'],'18:00',admin],['class4',course4,s.branchB,foreign,room3,['SATURDAY','SUNDAY'],'10:00',sa]]){
 const endTime=time==='10:00'?'11:30':'19:30';
 await keep(key,()=>ok('/academic/classes',actor,'POST',{code:key==='class2'?'GT-A2-A':key==='class3'?'IELTS-A':'TOEIC-A',name:key==='class2'?'Giao tiếp A2 A':key==='class3'?'IELTS A':'TOEIC A',courseId:c.id,branchId:b.id,teacherId:teach.profileId,teacherName:teach.fullName,roomId:room.id,startDate:s.nextMonday,endDate:shift(s.nextMonday,27),maxSize:15,schedules:days.map(dayOfWeek=>({dayOfWeek,startTime:time,endTime}))}));
}
await t.check('D01','Future classes include required entry and open-entry policies',['REQUIRES_ASSESSMENT','OPEN_ENTRY','OPEN_ENTRY'],async()=>[s.policy2.policy.mode,s.policy3.policy.mode,s.policy4.policy.mode]);
async function student(key,name,phone,branch=s.branchA.id,actor=staff){return keep(key,()=>ok('/academic/students',actor,'POST',{fullName:name,phone,branchId:branch,gender:'FEMALE',dateOfBirth:'2004-03-15',email:key+'@example.com',address:'TP.HCM'}));}
const missing=await student('missingStudent','Minh Khang','0901200001');
const wrongProgram=await student('wrongProgramStudent','Bảo Ngọc','0901200002');
async function assess(key,student,program,level){const draft=await keep(key+'Draft',()=>ok('/academic/students/'+student.id+'/assessments',staff,'POST',{programId:program.id,levelId:level.id,method:'INTERVIEW',assessedOn:s.today,assessorName:'GV An',source:'Biên bản phỏng vấn đầu vào '+student.fullName}));return keep(key,()=>ok('/academic/students/'+student.id+'/assessments/'+draft.id+'/confirm',staff,'POST',{version:draft.version,expectedCurrentId:null,reason:'Đã đối chiếu kết luận phỏng vấn'}));}
await assess('wrongProgramAssessment',wrongProgram,ielts,s.ieltsBasic);
const context=id=>({studentId:id,classId:s.class2.id,intent:'ENROLL'});
await t.check('D02','Missing, mismatched level and wrong-program assessments are distinguished',['MISSING_ASSESSMENT','LEVEL_MISMATCH','PROGRAM_MISMATCH'],async()=>{const list=[];for(const st of [missing,s.student,wrongProgram])list.push((await ok('/academic/enrollments/preview',staff,'POST',context(st.id))).academicStatus);return list;});
await t.check('D03','All three academically ineligible direct enrollments are blocked',[409,409,409],async()=>{const statuses=[];for(const st of [missing,s.student,wrongProgram])statuses.push((await api('/academic/enrollments',staff,'POST',{studentId:st.id,classId:s.class2.id})).status);return statuses;});
const rejected=await keep('rejectedException',async()=>{const preview=await ok('/academic/enrollments/preview',staff,'POST',context(missing.id));return ok('/academic/placement-exceptions',staff,'POST',{context:preview.context,fingerprint:preview.fingerprint,reason:'Đề nghị xét học viên chưa có đánh giá'});});
await t.check('D04','Staff cannot approve their own placement exception',403,async()=>(await api('/academic/placement-exceptions/'+rejected.id+'/decision',staff,'POST',{version:rejected.version,approve:true,reason:'Kiểm tra quyền phê duyệt'})).status);
await keep('rejection',()=>ok('/academic/placement-exceptions/'+rejected.id+'/decision',admin,'POST',{version:rejected.version,approve:false,reason:'Yêu cầu hoàn thành đánh giá đầu vào trước'}));
await t.check('D05','Rejected exception remains visible as rejected','REJECTED',async()=>(await ok('/academic/placement-exceptions/'+rejected.id,admin)).status);
await keep('pendingException',async()=>{const p=await ok('/academic/enrollments/preview',staff,'POST',context(wrongProgram.id));return ok('/academic/placement-exceptions',staff,'POST',{context:p.context,fingerprint:p.fingerprint,reason:'Chờ quản lý xem xét đánh giá khác chương trình'});});
await t.check('D06','An additional request is left pending for manual demonstration','REQUESTED',async()=>(await ok('/academic/placement-exceptions/'+s.pendingException.id,admin)).status);
s.futureEnrollments??=[];s.futureStudents??=[];saveState(s);
for(const [i,name,klass,branch,actor]of [[0,'Thu Hà',s.class2,s.branchA,staff],[1,'Hoàng Nam',s.class2,s.branchA,staff],[2,'Ngọc Linh',s.class3,s.branchA,staff],[3,'Gia Huy',s.class3,s.branchA,staff],[4,'Thanh Mai',s.class4,s.branchB,staffB],[5,'Đức Anh',s.class4,s.branchB,staffB]]){
 const st=await student('futureStudent'+i,name,'090130000'+(i+1),branch.id,actor);
 if(i<2)await assess('futureAssessment'+i,st,s.program,a1);
 s.futureStudents[i]=st;
 const e=await keep('futureEnrollment'+i,()=>ok('/academic/enrollments',actor,'POST',{studentId:st.id,classId:klass.id}));s.futureEnrollments[i]=e;saveState(s);
 await keep('futureTuition'+i,()=>ok('/finance/tuitions',actor,'POST',{enrollmentId:e.id}));
}
await t.check('D07','Confirmed A1 satisfies entry for the A2 course',true,async()=>{const h=await ok('/academic/enrollments/'+s.futureEnrollment0.id+'/admission-history',staff);return h[0].academicStatus==='ELIGIBLE'&&h[0].assessmentId===s.futureAssessment0.id;});
await t.check('D08','Enrollment evidence is produced by business services, not SQL fixture text',true,async()=>{const h=await ok('/academic/enrollments/'+s.futureEnrollment0.id+'/admission-history',staff);return h.length===1&&h[0].policySnapshot.startsWith('Policy[')&&!h[0].policySnapshot.includes('DEMO:')&&h[0].assessmentSnapshot.startsWith('AssessmentDto[')&&/^[a-f0-9]{64}$/.test(h[0].fingerprint);});
await t.check('D09','Three future classes each contain eight90minute sessions',[8,8,8,720,720,720],async()=>{const counts=[],minutes=[];for(const c of [s.class2,s.class3,s.class4]){const rows=await ok('/academic/sessions?size=200&classId='+c.id,sa);counts.push(rows.totalElements);minutes.push((await ok('/academic/classes/'+c.id+'/duration-progress',sa)).scheduledMinutes);}return [...counts,...minutes];});
await t.check('D10','Actual calendar sessions do not overlap teachers or rooms',0,async()=>{const rows=(await ok('/academic/sessions?size=200&from='+s.today+'&to='+shift(s.nextMonday,28),sa)).content;let collisions=0;for(let i=0;i<rows.length;i++)for(let j=i+1;j<rows.length;j++){const a=rows[i],b=rows[j];if(a.status!=='CANCELLED'&&b.status!=='CANCELLED'&&a.sessionDate===b.sessionDate&&a.startTime<b.endTime&&b.startTime<a.endTime&&(a.teacherId===b.teacherId||a.roomId===b.roomId))collisions++;}return collisions;});
for(const [key,tuition,amount,method]of [['futurePaymentPartial',s.futureTuition0,1000000,'CASH'],['futurePaymentFull',s.futureTuition1,3000000,'BANK_TRANSFER']])await keep(key,()=>ok('/finance/payments',staff,'POST',{tuitionId:tuition.id,amount,paymentMethod:method,requestKey:uuid(),note:'Thu học phí Giao tiếp A2'}));
await t.check('D11','Partial bill has1M paid and2M outstanding',[1000000,2000000,'PARTIAL'],async()=>{const bill=await ok('/finance/tuitions/'+s.futureTuition0.id,staff);return[bill.paidAmount,bill.balance,bill.status];});
await t.check('D12','Fully collected bill has zero balance',[3000000,0,'PAID'],async()=>{const bill=await ok('/finance/tuitions/'+s.futureTuition1.id,staff);return[bill.paidAmount,bill.balance,bill.status];});
await t.check('D13','Other branch retains an unpaid bill',[0,2500000],async()=>{const bill=await ok('/finance/tuitions/'+s.futureTuition4.id,staffB);return[bill.paidAmount,bill.balance];});
await t.check('D14','Global report matches expected7M receipts and18M outstanding',[7000000,18000000],async()=>{const r=await ok('/finance/stats/overview?from='+s.today+'&to='+s.today,sa);return[r.collected,r.outstanding];});
const finalPath='/teaching/classes/'+s.schoolClass.id+'/final-results';
s.oldSnapshot=(await ok(finalPath+'/'+s.enrollment.id+'/history',admin)).find(f=>f.id===s.finalA.id).snapshot;saveState(s);
await keep('reopenResults',async()=>{const f=await ok(finalPath,admin);return ok(finalPath+'/reopen',admin,'POST',{requestId:uuid(),expectedRevision:f.resultRevision,reason:'Đối chiếu và hiệu chỉnh bài kiểm tra'});});
await keep('reopenClass',async()=>{const c=await ok('/academic/classes/'+s.schoolClass.id+'/closure',admin);return ok('/academic/classes/'+s.schoolClass.id+'/reopen',admin,'POST',{requestId:uuid(),expectedRevision:c.revision,reason:'Mở lại để kiểm tra cập nhật kết quả'});});
await t.check('D15','Stale completion result cannot be reused after reopening',409,async()=>{const e=await ok('/academic/enrollments/'+s.enrollment.id,staff);return(await api('/academic/enrollments/'+e.id+'/status',staff,'PATCH',{studyStatus:'COMPLETED',version:e.version,finalResultId:s.finalA.id,finalResultRevision:s.finalA.revision,reason:'Kiểm tra căn cứ kết quả đã hết hiệu lực'})).status;});
await keep('correctedGrade',()=>ok('/teaching/grades',admin,'PUT',{examId:s.exam.id,studentId:s.student.id,score:86,reason:'Đối chiếu bài làm và cập nhật một điểm'}));
await t.check('D16','Original confirmed evidence is unchanged after correction',true,async()=>(await ok(finalPath+'/'+s.enrollment.id+'/history',admin)).find(f=>f.id===s.finalA.id).snapshot===s.oldSnapshot);
for(const [key,e]of [['currentFinalA',s.enrollment],['currentFinalB',s.enrollmentB]]){
 const f=await keep(key,async()=>{const p=await ok(finalPath+'/'+e.id+'/preview',admin,'POST');assert.equal(p.blockers.length,0);return ok(finalPath+'/'+e.id+'/confirm',admin,'POST',{requestId:uuid(),fingerprint:p.fingerprint,reason:'Xác nhận lại sau khi kiểm tra bài làm'});});
 await keep(key+'Completion',async()=>{const fresh=await ok('/academic/enrollments/'+e.id,staff);return ok('/academic/enrollments/'+e.id+'/status',staff,'PATCH',{studyStatus:'COMPLETED',version:fresh.version,finalResultId:f.id,finalResultRevision:f.revision,reason:'Cập nhật kết quả hiện hành cho đăng ký'});});
}
await t.check('D17','Reconfirmed final outcomes remain PASS and FAIL',['PASS','FAIL'],async()=>{const rows=await ok(finalPath,admin);return[s.enrollment.id,s.enrollmentB.id].map(id=>rows.results.find(f=>f.enrollmentId===id&&f.state==='CONFIRMED').outcome);});
await keep('currentClosure',async()=>{const c=await ok('/academic/classes/'+s.schoolClass.id+'/closure',admin);return ok('/academic/classes/'+s.schoolClass.id+'/close',admin,'POST',{requestId:uuid(),expectedRevision:c.revision,reason:'Đóng lại sau khi cập nhật đủ căn cứ'});});
await t.check('D18','Class ends in a verified closed state',[true,'VERIFIED'],async()=>{const c=await ok('/academic/classes/'+s.schoolClass.id+'/closure',admin);return[c.closed,c.verification];});
await t.check('D19','Completed enrollments point to the new confirmed result references',true,async()=>{const a=await ok('/academic/enrollments/'+s.enrollment.id,staff),b=await ok('/academic/enrollments/'+s.enrollmentB.id,staff);return a.studyStatus==='COMPLETED'&&b.studyStatus==='COMPLETED'&&a.completionResultId===s.currentFinalA.id&&b.completionResultId===s.currentFinalB.id;});
await t.check('D20','Learning history records actual enrollment and completion events',true,async()=>{const h=await ok('/academic/enrollments/'+s.enrollment.id+'/history',staff);return h.events.some(e=>e.action==='ENROLLED')&&h.events.some(e=>e.action==='COMPLETED')&&h.events.some(e=>e.action==='COMPLETION_RESULT_REFRESHED');});
s.phase4CompletedAt=new Date().toISOString();saveState(s);t.finish();
}catch(e){t.result.status='FAILED';t.result.error=e.message;t.save();console.error(e.message);process.exitCode=1;}


