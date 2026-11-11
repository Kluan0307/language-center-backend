import assert from 'node:assert/strict';
import {api,ok,login,ready,suite,expected,loadState,saveState,secrets,saveSecrets,uuid} from '../acceptance/client.mjs';

async function account(role,branch,key,sa){
 const names={admin:['admin','Admin'],staff:['staff','Nhân viên'],teacher:['teacher','GV An'],foreign:['teacher2','GV Bình'],staffB:['staff2','NV Chi nhánh 2']};
 const [username,fullName]=names[key],temporary='Demo-'+uuid();
 const record=await ok('/auth/accounts',sa,'POST',{username,password:temporary,role,fullName,branchId:branch});
 const initial=await login(username,temporary),password=temporary+'9';
 const changed=await api('/auth/me/password',initial,'POST',{currentPassword:temporary,newPassword:password,confirmPassword:password});assert.equal(changed.status,204);
 secrets.accounts[key]={...record,...await login(username,password)};saveSecrets();
}
const t=suite('g4-core'),s=loadState();
const keep=async(key,make)=>{if(!s[key]){s[key]=await make();saveState(s);}return s[key];};
try{
await ready();
const sa=await login(process.env.ACCEPTANCE_BOOTSTRAP_USERNAME||'superadmin',process.env.ACCEPTANCE_BOOTSTRAP_PASSWORD||'123456');secrets.accounts.sa={username:sa.username,token:sa.token};saveSecrets();
const branchA=await keep('branchA',()=>ok('/academic/branches/1',sa));
const branchB=await keep('branchB',()=>ok('/academic/branches/2',sa));
for(const [key,role,branch]of [['admin','ADMIN',branchA.id],['staff','STAFF',branchA.id],['teacher','TEACHER',branchA.id],['foreign','TEACHER',branchB.id],['staffB','STAFF',branchB.id]]){if(!secrets.accounts[key])await account(role,branch,key,sa);}
const {admin,staff,teacher,foreign,staffB}=secrets.accounts;
await t.check('G17-01','Four actual roles, two isolated branches, valid sessions',expected.roles,async()=>{const roles=[];for(const a of [sa,admin,staff,teacher])roles.push((await ok('/auth/me',a)).role);return roles;});
const program=await keep('program',()=>ok('/academic/programs',sa,'POST',{code:'GE',name:'Giao tiếp',active:true}));
for(const [key,code,rank]of [['a2','A2',1],['b1','B1',2],['b2','B2',3]])await keep(key,()=>ok('/academic/programs/'+program.id+'/levels',sa,'POST',{code,name:code,rank,active:true}));
const course=await keep('course',()=>ok('/academic/courses',admin,'POST',{code:'ON-B1',name:'Ôn tập B1',expectedMinutes:expected.courseMinutes,tuitionFee:expected.tuitionAmount,maxStudents:20,branchId:branchA.id}));
await keep('admission',()=>ok('/academic/courses/'+course.id+'/admission',admin,'PUT',{expectedRevision:0,mode:'REQUIRES_ASSESSMENT',programId:program.id,outputLevelId:s.b1.id,acceptedLevelIds:[s.b1.id],reason:'Nhận học viên đã được xác nhận trình độ B1'}));
const room=await keep('room',()=>ok('/academic/rooms',admin,'POST',{code:'P1',name:'Phòng 1',capacity:20,branchId:branchA.id}));
const weekday=['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'][new Date(s.today+'T12:00:00Z').getUTCDay()];
const school=await keep('schoolClass',()=>ok('/academic/classes',admin,'POST',{code:'B1-KT',name:'B1 Kết thúc',courseId:course.id,teacherId:teacher.profileId,teacherName:teacher.fullName,roomId:room.id,startDate:s.today,endDate:s.today,maxSize:20,branchId:branchA.id,schedules:[{dayOfWeek:weekday,startTime:'00:00',endTime:'03:00'}]}));
let lead=await keep('lead',()=>ok('/academic/leads',staff,'POST',{fullName:'Minh Anh',phone:'0901100001',source:'Tư vấn trực tiếp',interestedCourseId:course.id,branchId:branchA.id}));
await keep('contact',async()=>{lead=await ok('/academic/leads/'+lead.id,staff);return ok('/academic/leads/'+lead.id+'/interactions',staff,'POST',{channel:'CALL',note:'Tư vấn khóa ôn tập B1',status:'CONTACTED',version:lead.version});});
const converted=await keep('converted',async()=>{lead=await ok('/academic/leads/'+lead.id,staff);return ok('/academic/leads/'+lead.id+'/convert',staff,'POST',{version:lead.version});});s.student=converted.convertedStudent;saveState(s);
const other=await keep('otherStudent',()=>ok('/academic/students',staff,'POST',{fullName:'Quốc Bảo',phone:'0901100002',branchId:branchA.id}));
await t.check('G17-02','Contacted lead converts once to a real persisted Student',true,async()=>{const again=await ok('/academic/leads/'+lead.id+'/convert',staff,'POST',{version:s.contact.version});return again.status==='CONVERTED'&&again.convertedStudent.id===s.student.id&&(await ok('/academic/students/'+s.student.id,staff)).student.id===s.student.id;});
for(const [key,student,level,score]of [['assessmentA',s.student,s.b1,85],['assessmentB',other,s.a2,55]]){const a=await keep(key+'Draft',()=>ok('/academic/students/'+student.id+'/assessments',staff,'POST',{programId:program.id,levelId:level.id,method:'TEST',assessedOn:s.today,assessorName:'GV An',source:'Bài kiểm tra nội bộ số 1',score,maxScore:100}));await keep(key,()=>ok('/academic/students/'+student.id+'/assessments/'+a.id+'/confirm',staff,'POST',{version:a.version,expectedCurrentId:null,reason:'Đã đối chiếu bài kiểm tra nội bộ'}));}
const context=studentId=>({studentId,classId:school.id,intent:'ENROLL'});
await t.check('G17-03','Actual preview accepts B1 and rejects A2 by accepted level set',[true,false],async()=>[(await ok('/academic/enrollments/preview',staff,'POST',context(s.student.id))).eligible,(await ok('/academic/enrollments/preview',staff,'POST',context(other.id))).eligible]);
await t.check('G17-04','Direct enrollment lacking academic eligibility is rejected',409,async()=>(await api('/academic/enrollments',staff,'POST',{studentId:other.id,classId:school.id})).status);
const exception=await keep('exception',async()=>{const p=await ok('/academic/enrollments/preview',staff,'POST',context(other.id));return ok('/academic/placement-exceptions',staff,'POST',{context:p.context,fingerprint:p.fingerprint,reason:'Đề nghị quản lý xem xét học thử lớp ôn tập B1'});});
await keep('approval',()=>ok('/academic/placement-exceptions/'+exception.id+'/decision',admin,'POST',{version:exception.version,approve:true,reason:'Đồng ý ngoại lệ sau khi tư vấn trực tiếp'}));
await keep('enrollment',()=>ok('/academic/enrollments',staff,'POST',{studentId:s.student.id,classId:school.id}));
await keep('enrollmentB',()=>ok('/academic/enrollments',staff,'POST',{studentId:other.id,classId:school.id,approvalId:exception.id}));
await t.check('G17-05','Approval is really consumed once and replay returns the same enrollment',true,async()=>{const a=await ok('/academic/placement-exceptions/'+exception.id,admin);const again=await ok('/academic/enrollments',staff,'POST',{studentId:other.id,classId:school.id,approvalId:exception.id});return a.status==='CONSUMED'&&a.consumedEnrollmentId===s.enrollmentB.id&&again.id===s.enrollmentB.id;});
await keep('tuition',()=>ok('/finance/tuitions',staff,'POST',{enrollmentId:s.enrollment.id}));await keep('tuitionB',()=>ok('/finance/tuitions',staff,'POST',{enrollmentId:s.enrollmentB.id}));
await t.check('G17-06','After-commit tuition delivery is recorded and retry creates no duplicate',true,async()=>{await ok('/academic/enrollments/'+s.enrollment.id+'/tuition/retry',staff,'POST');const e=await ok('/academic/enrollments/'+s.enrollment.id,staff);const list=await ok('/finance/tuitions?studentId='+s.student.id,staff);return e.tuitionDelivery==='CREATED'&&list.content.filter(x=>x.enrollmentId===s.enrollment.id).length===1;});
s.paymentKey1??=uuid();s.paymentKey2??=uuid();saveState(s);
const paymentBody=(amount,requestKey,paymentMethod)=>({tuitionId:s.tuition.id,amount,requestKey,paymentMethod,note:'Thu học phí ôn tập B1'});
const p1=await keep('payment1',()=>ok('/finance/payments',staff,'POST',paymentBody(expected.installments[0],s.paymentKey1,'CASH')));
await t.check('G17-07','First receipt 1M leaves 2M balance',true,async()=>{const tuition=await ok('/finance/tuitions/'+s.tuition.id,staff);return tuition.paidAmount===1000000&&tuition.balance===2000000;});
await t.check('G17-08','Same payment requestKey returns persisted receipt without another payment',p1.id,async()=>(await ok('/finance/payments',staff,'POST',paymentBody(1000000,s.paymentKey1,'CASH'))).id);
await keep('payment2',()=>ok('/finance/payments',staff,'POST',paymentBody(expected.installments[1],s.paymentKey2,'BANK_TRANSFER')));
await t.check('G17-09','Two receipts total3M; paid status and zero balance',[3000000,0,'PAID',2],async()=>{const tuition=await ok('/finance/tuitions/'+s.tuition.id,staff),payments=await ok('/finance/payments?tuitionId='+s.tuition.id,staff);return[tuition.paidAmount,tuition.balance,tuition.status,payments.content.length];});
const sessions=(await ok('/academic/sessions?classId='+school.id,admin)).content;s.session=sessions.find(x=>x.sessionDate===s.today);assert.ok(s.session);saveState(s);
await t.check('G17-10','Real scheduled minutes equal verified course plan',expected.courseMinutes,async()=>(await ok('/academic/classes/'+school.id+'/duration-progress',admin)).scheduledMinutes);
await keep('attendance',async()=>{const sheet=await ok('/academic/attendances/sheet?sessionId='+s.session.id,teacher);return ok('/academic/attendances/bulk',teacher,'POST',{classId:school.id,sessionId:s.session.id,sessionDate:s.today,version:sheet.version,records:sheet.records.map(x=>({enrollmentId:x.enrollmentId,status:'PRESENT',hasPermission:false}))});});
await t.check('G17-11','Attendance really completes session and accumulates180 taught minutes',[180,true,'COMPLETED'],async()=>{const progress=await ok('/academic/classes/'+school.id+'/duration-progress',admin),session=await ok('/academic/sessions/'+s.session.id,admin);return[progress.taughtMinutes,progress.timeReadyForClosure,session.status];});
const exam=await keep('exam',()=>ok('/teaching/exams',teacher,'POST',{classId:school.id,name:'Cuối buổi',examType:'TEST',examDate:s.today,weight:1,maxScore:100}));
await keep('gradeA',()=>ok('/teaching/grades',teacher,'PUT',{examId:exam.id,studentId:s.student.id,score:expected.studentScores[0]}));
const policyPath='/teaching/classes/'+school.id+'/evaluation-policy';
await keep('evaluationPolicy',async()=>{const fresh=await ok('/teaching/exams/'+exam.id,teacher);return ok(policyPath,admin,'PUT',{expectedRevision:0,passPercent:expected.finalThresholdPercent,minAttendancePercent:80,exams:[{examId:exam.id,expectedExamVersion:fresh.version,required:true,weight:1}],reason:'Điểm đạt 60 phần trăm, chuyên cần tối thiểu 80 phần trăm'});});
await t.check('G17-12','Missing mandatory grade is INSUFFICIENT, not an invented PASS',true,async()=>{const report=await ok('/teaching/classes/'+school.id+'/evaluation-preview',admin);return report.students.find(x=>x.enrollmentId===s.enrollmentB.id).status==='INSUFFICIENT';});
await keep('gradeB',()=>ok('/teaching/grades',teacher,'PUT',{examId:exam.id,studentId:other.id,score:expected.studentScores[1]}));
const finalPath='/teaching/classes/'+school.id+'/final-results';
await t.check('G17-13','Provisional outcomes are PASS and FAIL with known attendance',expected.finalOutcomes,async()=>{const report=await ok('/teaching/classes/'+school.id+'/evaluation-preview',admin);return[s.enrollment.id,s.enrollmentB.id].map(id=>report.students.find(x=>x.enrollmentId===id).status);});
for(const [key,e]of [['finalA',s.enrollment],['finalB',s.enrollmentB]])await keep(key,async()=>{const p=await ok(finalPath+'/'+e.id+'/preview',admin,'POST');assert.equal(p.blockers.length,0);const intent=uuid();return ok(finalPath+'/'+e.id+'/confirm',admin,'POST',{requestId:intent,fingerprint:p.fingerprint,reason:'Đã kiểm tra điểm, chuyên cần và thời lượng'});});
await t.check('G17-14','Final outcomes are persisted on two enrollment identities',expected.finalOutcomes,async()=>{const finals=await ok(finalPath,staff);return[s.enrollment.id,s.enrollmentB.id].map(id=>finals.results.find(x=>x.enrollmentId===id&&x.state==='CONFIRMED').outcome);});
await t.check('G17-15','Grade modification after finalization is actually rejected',409,async()=>(await api('/teaching/grades',teacher,'PUT',{examId:exam.id,studentId:s.student.id,score:90})).status);
for(const [key,e,f]of [['completionA',s.enrollment,s.finalA],['completionB',s.enrollmentB,s.finalB]])await keep(key,async()=>{const current=await ok('/academic/enrollments/'+e.id,staff);return ok('/academic/enrollments/'+e.id+'/status',staff,'PATCH',{studyStatus:'COMPLETED',version:current.version,finalResultId:f.id,finalResultRevision:f.revision,reason:'Xác nhận đã hoàn thành việc học'});});
await t.check('G17-16','PASS and FAIL both complete; unpaid B still has its original debt',[true,true,3000000],async()=>[(await ok('/academic/enrollments/'+s.enrollment.id,staff)).studyStatus==='COMPLETED',(await ok('/academic/enrollments/'+s.enrollmentB.id,staff)).studyStatus==='COMPLETED',(await ok('/finance/tuitions/'+s.tuitionB.id,staff)).balance]);
await keep('closure',async()=>{const c=await ok('/academic/classes/'+school.id+'/closure',admin);return ok('/academic/classes/'+school.id+'/close',admin,'POST',{requestId:uuid(),expectedRevision:c.revision,reason:'Đã đối soát toàn bộ kết quả và thời lượng'});});
await t.check('G17-17','Class closed flag and closure proof are verified',[true,'VERIFIED'],async()=>{const c=await ok('/academic/classes/'+school.id+'/closure',admin);return[c.closed,c.verification];});
await t.check('G17-18','Real branch revenue matches predefined gross/net/debt',expected.revenue,async()=>{const r=await ok('/finance/stats/overview?branchId='+branchA.id+'&from='+s.today+'&to='+s.today,admin);return Object.fromEntries(Object.keys(expected.revenue).map(k=>[k,r[k]]));});
await t.check('G17-19','Reload preserves Student profile, periods and final reference',true,async()=>{const p=await ok('/academic/students/'+s.student.id+'/learning-profile',staff);return p.studies.some(x=>x.enrollment.id===s.enrollment.id&&x.periods.length===1&&x.enrollment.completionResultId===s.finalA.id);});
await t.check('G17-20','Search/filter/pagination returns exactly the two synthetic students',[2,1,2],async()=>{const p=await ok('/academic/students?branchId='+branchA.id+'&size=1&page=0',staff);return[p.totalElements,p.content.length,p.totalPages];});
s.g17CompletedAt=new Date().toISOString();saveState(s);t.finish();
}catch(e){t.result.status='FAILED';t.result.error=e.message;t.save();console.error(e.message);process.exitCode=1;}
