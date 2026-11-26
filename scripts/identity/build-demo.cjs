const fs=require('fs'),path=require('path'),crypto=require('crypto'),{execFileSync}=require('child_process');
const root=path.resolve(__dirname,'../..');process.chdir(root);
const jar=path.join(process.env.USERPROFILE,'.m2/repository/org/springframework/security/spring-security-crypto/7.1.1/spring-security-crypto-7.1.1.jar');
const hash=execFileSync('java',['--class-path',jar,'scripts/identity/DemoPassword.java'],{encoding:'utf8',windowsHide:true}).trim();
const parts=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts();
const today=['year','month','day'].map(k=>parts.find(p=>p.type===k).value).join('-');
const date=(d,n=0)=>{const x=new Date(d+'T00:00:00Z');x.setUTCDate(x.getUTCDate()+n);return x.toISOString().slice(0,10);};
const monday=date(today,-((new Date(today+'T00:00:00Z').getUTCDay()+6)%7));
const start=date(monday,-7),upcoming=date(monday,7),registered=date(start,-3),stamp=registered+' 09:00:00';
const statements=['-- Generated demo fixture. Empty database only; use scripts/identity/load-demo.cjs.','SET NAMES utf8mb4;','START TRANSACTION;'];
const literal=v=>v===null?'NULL':typeof v==='number'?String(v):"'"+String(v).replace(/\\/g,'\\\\').replace(/'/g,"''")+"'";
function add(table,row){statements.push('INSERT INTO `'+table+'` ('+Object.keys(row).map(k=>'`'+k+'`').join(',')+') VALUES ('+Object.values(row).map(literal).join(',')+');');}
const sha=v=>crypto.createHash('sha256').update(typeof v==='string'?v:JSON.stringify(v)).digest('hex');
const people=[['superadmin','SUPER_ADMIN','Quản trị hệ thống',null],['admin1','ADMIN','Nguyễn Minh An',1],['admin2','ADMIN','Trần Thu Hà',2],['staff1','STAFF','Lê Ngọc Mai',1],['staff2','STAFF','Phạm Văn Nam',1],['staff3','STAFF','Võ Thu Linh',2],['staff4','STAFF','Đặng Minh Tú',2],['teacher1','TEACHER','Nguyễn Hải Anh',1],['teacher2','TEACHER','Trần Bảo Ngọc',1],['teacher3','TEACHER','Lê Quang Minh',2],['teacher4','TEACHER','Phạm Thùy Dung',2]];
for(let b=1;b<=2;b++)add('branches',{id:b,code:'CN0'+b,name:'Cơ sở '+b,address:b===1?'Quận 1, TP.HCM':'Thủ Đức, TP.HCM',phone:'028000000'+b,status:'ACTIVE',created_at:stamp});
let assignment=0,personnel=0;
const teacherNames={};
people.forEach(([username,role,name,branch],index)=>{
 const id=index+1,code=role==='SUPER_ADMIN'?null:(role==='TEACHER'?'GV':'NS')+String(++personnel).padStart(3,'0');
 add('accounts',{id,username,password:hash,role,status:1,branch_id:branch,personnel_code:code,created_at:stamp,session_version:0,must_change_password:0});
 if(role==='SUPER_ADMIN')add('super_admins',{id:1,account_id:id,full_name:name,email:username+'@example.com',phone:'0900000001'});
 else {
  const profileId=role==='ADMIN'?index:role==='STAFF'?index-2:index-6;
  const table=role==='ADMIN'?'admins':role==='STAFF'?'staffs':'teachers';
  add(table,{id:profileId,account_id:id,full_name:name,email:username+'@example.com',phone:'090'+String(id).padStart(7,'0'),hire_date:date(registered,-365),end_date:null,work_status:'ACTIVE',version:0,...(role==='STAFF'?{position:'Tư vấn đào tạo'}:{}),...(role==='TEACHER'?{specialization:profileId===2||profileId===3?'IELTS':'Tiếng Anh tổng quát',qualification:'Cử nhân',certificate:profileId===2||profileId===3?'IELTS 8.0':'TESOL',experience:'3 năm'}:{})});
  if(role==='TEACHER')teacherNames[profileId]=name;
  const ids=username==='admin2'||username==='teacher1'||username==='teacher4'?[1,2]:[branch];
  for(const b of ids){add('account_branches',{id:++assignment,account_id:id,branch_id:b,status:1,is_primary:b===branch?1:0,assigned_at:stamp,assigned_by:1});add('account_branch_events',{id:assignment,account_id:id,branch_id:b,action:'ASSIGNED',performed_by:1,occurred_at:stamp});}
 }
});
add('personnel_code_numbers',{id:personnel});
for(const [id,code,name] of [[1,'GE','Tiếng Anh tổng quát'],[2,'IELTS','IELTS'],[3,'TOEIC','TOEIC']])add('programs',{id,code,name,active:1,version:0});
for(const [id,program,code,name,sort] of [[1,1,'A1','A1',1],[2,1,'A2','A2',2],[3,1,'B1','B1',3],[4,2,'BASIC','Nền tảng',1],[5,2,'6.5','Mục tiêu 6.5',2],[6,3,'BASIC','Nền tảng',1],[7,3,'450','Mục tiêu 450',2]])add('program_levels',{id,program_id:program,code,name,sort_order:sort,active:1,version:0});
const courseNames={},fees={},policy={};
for(let id=1;id<=6;id++){
 const branch=id<=3?1:2,kind=(id-1)%3,program=kind===2?2:1,level=kind===2?4:kind+1,code=kind===2?'IELTS-F':'GE-A'+(kind+1),name=kind===2?'IELTS nền tảng':'Tiếng Anh A'+(kind+1),fee=[2000000,2500000,4500000][kind];courseNames[id]=name;fees[id]=fee;
 add('courses',{id,branch_id:branch,code,name,type:program===1?'GE':'IELTS',level:kind===2?'BASIC':'A'+(kind+1),duration:null,duration_revision:1,expected_minutes:720,tuition_fee:fee,max_students:15,status:'OPEN',content:'8 buổi, mỗi buổi 90 phút',entry_requirement:'Khóa nhập môn; tư vấn trước khi đăng ký'});
 add('course_duration_plans',{id,course_id:id,revision:1,expected_minutes:720,actor_id:1,actor_name:'superadmin',occurred_at:stamp,reason:'Kế hoạch khóa học đã xác nhận'});
 policy[id]={mode:'OPEN_ENTRY',programId:program,programCode:program===1?'GE':'IELTS',outputLevelId:level,outputLevelCode:kind===2?'BASIC':'A'+(kind+1)};
 add('course_admission_policies',{id,course_id:id,revision:1,mode:'OPEN_ENTRY',program_id:program,program_code:policy[id].programCode,program_name:program===1?'Tiếng Anh tổng quát':'IELTS',output_level_id:level,output_level_code:policy[id].outputLevelCode,output_level_name:kind===2?'Nền tảng':'A'+(kind+1),confirmed_by:1,confirmed_by_name:'superadmin',confirmed_at:stamp,reason:'Khóa nhập môn không yêu cầu đánh giá bắt buộc'});
}
for(let id=1;id<=6;id++)add('rooms',{id,branch_id:id<=3?1:2,code:'P10'+((id-1)%3+1),name:'Phòng 10'+((id-1)%3+1),capacity:20,status:'AVAILABLE',location:'Tầng 1',equipment:'TV, bảng, loa'});
const configs=[[1,1,1,1,[0,2],'18:00:00'],[1,2,2,2,[1,3],'18:00:00'],[1,3,2,3,[0,2],'08:00:00'],[1,1,4,1,[1,3],'08:00:00'],[2,4,1,4,[1,3],'18:00:00'],[2,5,3,5,[0,2],'18:00:00'],[2,6,3,6,[1,3],'08:00:00'],[2,4,4,4,[0,2],'08:00:00']];
const classes=[],sessions=[];let scheduleId=0,sessionId=0;
configs.forEach(([branch,course,teacher,room,days,time],index)=>{
 const id=index+1,isUpcoming=id===4||id===8,base=isUpcoming?upcoming:start,first=date(base,days[0]),last=date(base,21+days[1]),code=['A1-01','A2-01','IELTS-01','A1-02'][(id-1)%4],name=courseNames[course]+' '+(isUpcoming?'buổi sáng':time.startsWith('18')?'buổi tối':'buổi sáng');
 classes.push({id,branch,course,teacher,room,days,time,first,last,code,name,isUpcoming});
 add('school_classes',{id,branch_id:branch,code,name,current_size:0,max_size:15,start_date:first,end_date:last,status:isUpcoming?'UPCOMING':'ONGOING',status_manually_set:0,teacher_id:teacher,teacher_name:teacherNames[teacher],course_id:course,room_id:room,session_end_date:last,sessions_initialized:1,admission_policy_id:course,admission_revision:1,duration_revision:1,expected_minutes:720});
 add('class_duration_plans',{id,class_id:id,course_id:course,course_revision:1,expected_minutes:720,actor_id:1,actor_name:'superadmin',occurred_at:stamp,reason:'Theo khóa học',revision:1,source:'COURSE'});
 for(const day of days){const sid=++scheduleId,end=time.startsWith('18')?'19:30:00':'09:30:00';add('schedules',{id:sid,class_id:id,day_of_week:['MONDAY','TUESDAY','WEDNESDAY','THURSDAY'][day],start_time:time,end_time:end});
  for(let week=0;week<4;week++){const when=date(base,week*7+day),completed=when<today;const row={id:++sessionId,class_id:id,session_date:when,start_time:time,end_time:end,room_id:room,room_name:'Phòng 10'+((room-1)%3+1),planned_teacher_id:teacher,planned_teacher_name:teacherNames[teacher],teacher_id:teacher,teacher_name:teacherNames[teacher],status:completed?'COMPLETED':'SCHEDULED',kind:'REGULAR',source_date:when,source_schedule_id:sid,customized:0,version:0,created_at:stamp,updated_at:stamp};sessions.push(row);add('class_sessions',row);}
 }
});
const names=['Nguyễn Minh Anh','Trần Quốc Bảo','Lê Thu Hà','Phạm Hoàng Nam','Võ Ngọc Linh','Đặng Gia Huy','Bùi Khánh Vy','Đỗ Đức Anh','Hồ Thanh Tâm','Ngô Mai Chi','Dương Tuấn Kiệt','Lý Ngọc Hân','Trịnh Minh Khang','Phan Bảo Trâm','Vũ Nhật Minh','Nguyễn Hải Đăng','Trần Hoài An','Lê Bảo Ngọc','Phạm Minh Quân','Võ Thanh Thảo','Đặng Anh Khoa','Bùi Thùy Dương','Đỗ Quỳnh Anh','Hồ Đức Huy','Ngô Kim Ngân','Dương Gia Bảo','Lý Khánh Linh','Trịnh Nhật Anh','Phan Minh Châu','Vũ Tuấn Anh'];
const enrollments=[];let enrollmentId=0,paymentId=0;
for(let id=1;id<=30;id++){
 const branch=id<=15?1:2,local=(id-1)%15,minor=id===3||id===18,paused=id===13,account=11+id,code='HV'+String(id).padStart(4,'0');
 add('accounts',{id:account,username:code.toLowerCase(),password:hash,role:'STUDENT',status:1,branch_id:branch,created_at:stamp,session_version:0,must_change_password:0});
 add('students',{id,account_id:account,branch_id:branch,student_code:code,full_name:names[id-1],date_of_birth:minor?'2012-05-15':'2003-06-20',gender:id%2?'FEMALE':'MALE',email:'hv'+id+'@example.com',phone:'090'+String(id===2?1:id).padStart(7,'0'),address:branch===1?'Quận 1, TP.HCM':'Thủ Đức, TP.HCM',registered_at:registered,status:paused?'PAUSED':'STUDYING',current_level:null,guardian_name:minor?'Nguyễn Thanh Bình':null,guardian_phone:minor?'0910000001':null,version:0});
 if(local===14)continue;
 const cls=classes[(branch-1)*4+local%4],eid=++enrollmentId,fee=fees[cls.course],paid=local%3===0?fee:local%3===1?Math.floor(fee/2):0,due=date(cls.first,7);
 enrollments.push({id:eid,student:id,cls:cls.id,name:names[id-1],paused});
 add('enrollments',{id:eid,class_id:cls.id,student_id:id,enrolled_at:stamp,study_status:paused?'RESERVED':'STUDYING',tuition_fee:fee,registered_by:branch===1?4:6,registered_by_name:branch===1?'staff1':'staff3',tuition_delivery:'CREATED',reserved_since:paused?date(today,-2):null,expected_return_date:paused?date(today,14):null,version:0});
 add('enrollment_periods',{id:eid,enrollment_id:eid,active_from:cls.first,active_until:paused?date(today,-3):null});
 add('enrollment_admissions',{id:eid,enrollment_id:eid,student_id:id,class_id:cls.id,class_revision:1,policy_id:cls.course,academic_status:'ELIGIBLE',intent:'ENROLL',fingerprint:sha('demo-admission-'+eid),policy_snapshot:JSON.stringify(policy[cls.course]),assessment_snapshot:'NONE',actor_id:branch===1?4:6,actor_name:branch===1?'staff1':'staff3',accepted_at:stamp});
 add('tuition',{id:eid,enrollment_id:eid,student_id:id,student_name:names[id-1],class_id:cls.id,class_name:cls.name,course_id:cls.course,course_name:courseNames[cls.course],branch_id:branch,currency:'VND',original_amount:fee,discount_amount:0,amount:fee,paid_amount:paid,due_date:due,status:paid===fee?'PAID':paid>0?'PARTIAL':due<today?'OVERDUE':'UNPAID',version:0});
 if(paid>0)add('payments',{id:++paymentId,tuition_id:eid,branch_id:branch,amount:paid,status:'PAID',payment_method:'CASH',paid_at:registered+' 10:00:00',receipt_no:'PT'+String(paymentId).padStart(6,'0'),request_key:'demo-receipt-'+eid,recorded_by:branch===1?4:6,recorded_by_name:branch===1?'staff1':'staff3'});
}
add('student_code_numbers',{id:30});
for(const [index,student] of [1,3,18].entries()){
 const isIelts=student!==1,branch=student<=15?1:2;
 add('placement_assessments',{id:index+1,student_id:student,branch_id:branch,program_id:isIelts?2:1,program_code:isIelts?'IELTS':'GE',program_name:isIelts?'IELTS':'Tiếng Anh tổng quát',level_id:isIelts?4:1,level_code:isIelts?'BASIC':'A1',level_name:isIelts?'Nền tảng':'A1',method:'INTERVIEW',assessed_on:registered,assessor_name:teacherNames[isIelts?(branch===1?2:3):1],source:'Phỏng vấn trực tiếp tại trung tâm',status:'CONFIRMED',confirmed_by:1,confirmed_by_name:'superadmin',confirmed_at:stamp,recorded_by:1,recorded_by_name:'superadmin',created_at:stamp,updated_at:stamp,version:0});
}
const reserved=enrollments.find(e=>e.student===13);
add('learning_events',{id:1,branch_id:1,student_id:13,enrollment_id:reserved.id,action:'RESERVE',old_status:'STUDYING',new_status:'RESERVED',reason:'Học viên tạm nghỉ theo đề nghị gia đình',effective_date:date(today,-2),expected_return_date:date(today,14),actor_id:4,actor_name:'staff1',occurred_at:date(today,-2)+' 09:00:00'});
let attendance=0;
for(const cls of classes){const members=enrollments.filter(e=>e.cls===cls.id);statements.push('UPDATE school_classes SET current_size='+members.filter(e=>!e.paused).length+' WHERE id='+cls.id+';');
 for(const session of sessions.filter(s=>s.class_id===cls.id&&s.status==='COMPLETED'))for(const e of members){if(e.paused&&session.session_date>=date(today,-2))continue;add('attendances',{id:++attendance,enrollment_id:e.id,class_session_id:session.id,session_date:session.session_date,status:e.student%7===0?'ABSENT':'PRESENT',has_permission:0,recorded_by:cls.teacher+7,recorded_by_name:teacherNames[cls.teacher]});}
}
// Two real publication snapshots for IELTS classes; no fabricated provider transaction.
let candidate=0,grade=0,published=0;
for(const [index,cls] of [classes[2],classes[6]].entries()){
 const id=index+1,examDate=date(start,7+(cls.days[0])),members=enrollments.filter(e=>e.cls===cls.id),examName='Thi thử IELTS',examType='IELTS',comment='Đã đánh giá';
 add('exams',{id,class_id:cls.id,branch_id:cls.branch,name:examName,exam_type:examType,exam_date:examDate,max_score:9,weight:1,created_at:stamp,created_by:cls.teacher+7,created_by_name:teacherNames[cls.teacher],roster_frozen_at:examDate+' 10:00:00',roster_frozen_by:cls.teacher+7,roster_frozen_by_name:teacherNames[cls.teacher],version:0});
 const cs=[],gs=[];
 for(const [i,e] of members.entries()){const score=5.5+(i%3)*0.5;add('exam_candidates',{id:++candidate,exam_id:id,student_id:e.student,student_name:e.name,enrollment_id:e.id,source:'ENROLLMENT_PERIOD'});add('grades',{id:++grade,exam_id:id,student_id:e.student,score,student_comment:comment,entered_by:cls.teacher+7,entered_at:examDate+' 11:00:00',branch_id:cls.branch,version:0});cs.push([e.student,e.id,'ENROLLMENT_PERIOD']);gs.push([e.student,score,comment,0]);}
 cs.sort((a,b)=>a[0]-b[0]);gs.sort((a,b)=>a[0]-b[0]);const draft=sha([examName,examType,examDate,9,1,true,cs,gs]);
 add('grade_publications',{id,exam_id:id,revision:1,request_id:crypto.randomUUID(),fingerprint:sha([id,'PUBLISH',0,draft,'Công bố kết quả']),draft_fingerprint:draft,action:'PUBLISH',reason:'Công bố kết quả',published_by:cls.teacher+7,published_at:examDate+' 12:00:00',class_id:cls.id,branch_id:cls.branch,exam_name:examName,exam_type:examType,exam_date:examDate,max_score:9,weight:1});
 for(const e of members){add('published_grades',{id:++published,publication_id:id,enrollment_id:e.id,student_id:e.student,score:gs.find(g=>g[0]===e.student)[1],student_comment:comment});}
}
for(let id=1;id<=6;id++)add('leads',{id,branch_id:id<=3?1:2,full_name:['Nguyễn Hoàng Anh','Trần Minh Tâm','Lê Gia Hân','Phạm Quang Huy','Võ Ngọc An','Đặng Thu Trang'][id-1],phone:'092'+String(id).padStart(7,'0'),email:'lead'+id+'@example.com',source:'WEBSITE',status:id%2?'NEW':'CONTACTED',assigned_to:id<=3?4:6,assigned_to_name:id<=3?'staff1':'staff3',next_contact_at:today+' '+(id%2?'09':'15')+':00:00',created_at:stamp,updated_at:stamp,version:0});
for(let id=1;id<=2;id++){const student=id===1?1:16,branch=id;add('student_support_requests',{id,branch_id:branch,student_id:student,category:'PROFILE',subject:'Cập nhật thông tin liên hệ',status:'OPEN',created_at:today+' 08:00:00',updated_at:today+' 08:00:00',version:0});}
statements.push('COMMIT;');
fs.writeFileSync('docs/demo/seed-identity-full.sql',statements.join('\n')+'\n');
fs.writeFileSync('docs/demo/identity-demo-manifest.json',JSON.stringify({generatedFor:today,password:'Demo@123456',accounts:41,branches:2,personnel:10,teachers:4,staff:4,admins:2,students:30,courses:6,classes:8,sessions:sessions.length,enrollments:enrollments.length,receipts:paymentId,publications:2,unregisteredStudents:['HV0015','HV0030'],minorStudents:['HV0003','HV0018'],reservedStudent:'HV0013',studentWithPublishedGrades:'hv0003',sharedBranchTeachers:['teacher1','teacher4'],start,upcoming},null,2)+'\n');
console.log('Generated empty-database-only fixture and manifest for '+today);
