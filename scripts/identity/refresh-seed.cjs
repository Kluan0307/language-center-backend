const {fs,path,dir,root,query,assert,literal:q,sha}=require('./refresh-common.cjs');
const {draftFingerprint}=require('./grade-fingerprints.cjs');
const plan=JSON.parse(fs.readFileSync(path.join(dir,'plan.json'))),backup=JSON.parse(fs.readFileSync(path.join(dir,'backup.json')));
assert.ok(backup.restoreVerified,'D2 restore must pass before D3');
const today=process.env.REFRESH_AS_OF||new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
assert.match(today,/^\d{4}-\d{2}-\d{2}$/);
const date=(d,n)=>{const t=new Date(d+'T00:00:00Z');t.setUTCDate(t.getUTCDate()+n);return t.toISOString().slice(0,10);};
const monday=date(today,-((new Date(today+'T00:00:00Z').getUTCDay()+6)%7)),start=date(monday,-14),historical=date(monday,-70),upcoming=date(monday,28),registered=date(historical,-5),stamp=registered+' 09:00:00',now=today+' 08:00:00';
const inv=JSON.parse(fs.readFileSync(path.join(dir,'inventory.json'))),max={...plan.highWater},counts={};
const next=t=>(max[t]=(max[t]??0)+1),rows={},sql=['SET NAMES utf8mb4;','CREATE TEMPORARY TABLE refresh_assertions(label VARCHAR(120),ok INT NOT NULL CHECK(ok=1));','START TRANSACTION;'];
sql.push("INSERT INTO refresh_assertions VALUES ('exclusive refresh lock',IF(GET_LOCK(CONCAT('lc_refresh:',DATABASE()),0)=1,1,0));");
const accountVersions=plan.accounts.map(a=>'('+a.id+','+Number(a.session_version??0)+')').join(',');
sql.push("INSERT INTO refresh_assertions VALUES ('account version precondition',IF((SELECT COUNT(*) FROM accounts WHERE (id,COALESCE(session_version,0)) IN ("+accountVersions+'))='+plan.accounts.length+',1,0));');
for(const t of plan.reset)sql.push('DELETE FROM `'+t+'`;');
function add(t,row){for(const k of Object.keys(row))assert.ok(inv.columns.some(c=>c.table===t&&c.column===k&&!/GENERATED/.test(c.extra)),t+'.'+k);sql.push('INSERT INTO `'+t+'` ('+Object.keys(row).map(k=>'`'+k+'`').join(',')+') VALUES ('+Object.values(row).map(q).join(',')+');');(rows[t]??=[]).push(row);counts[t]=(counts[t]||0)+1;return row;}
const update=(t,id,r)=>sql.push('UPDATE `'+t+'` SET '+Object.entries(r).map(([k,v])=>'`'+k+'`='+q(v)).join(',')+' WHERE id='+id+';');
const uuid=s=>{const h=sha(s);return h.slice(0,8)+'-'+h.slice(8,12)+'-4'+h.slice(13,16)+'-a'+h.slice(17,20)+'-'+h.slice(20,32);};
const sa=plan.accounts.find(a=>a.role==='SUPER_ADMIN');assert.ok(sa);
function level(programCode,code,name){const p=plan.programs.find(p=>p.code===programCode);assert.ok(p);if(name===code)name=code==='BASIC'?'Cơ bản':code==='ADVANCED'?'Nâng cao':code==='PRE1'?'Làm quen 1':code==='PRE2'?'Làm quen 2':programCode==='IELTS'||programCode==='TOEIC'?'Mục tiêu '+code:code;let l=plan.levels.find(l=>l.program_id===p.id&&l.code===code);if(!l){l=add('program_levels',{id:next('program_levels'),program_id:p.id,code,name,sort_order:10+plan.levels.filter(l=>l.program_id===p.id).length,active:1,version:0});plan.levels.push(l);}return l;}
const definitions=[
 ['MG1','Mẫu giáo 1','GE',['PRESCHOOL'],'PRE1',null,1800000,60],
 ['MG2','Mẫu giáo 2','GE',['PRESCHOOL'],'PRE2','PRE1',2000000,60],
 ['TH1','Tiểu học 1','GE',['PRIMARY'],'A1',null,2200000,90],
 ['TH2','Tiểu học 2','GE',['PRIMARY'],'A2','A1',2500000,90],
 ['CS1','Tiếng Anh A1','GE',['SECONDARY','HIGH_SCHOOL','ADULT'],'A1',null,2600000,90],
 ['PT1','Tiếng Anh B1','GE',['HIGH_SCHOOL','ADULT'],'B1','A2',3000000,90],
 ['GT1','Giao tiếp cơ bản','COMMUNICATION',['ADULT'],'BASIC',null,2800000,90],
 ['GT2','Giao tiếp nâng cao','COMMUNICATION',['HIGH_SCHOOL','ADULT'],'ADVANCED','BASIC',3500000,90],
 ['IL1','IELTS nền tảng','IELTS',['SECONDARY','HIGH_SCHOOL','ADULT'],'BASIC',null,4000000,90],
 ['IL2','IELTS 5.5','IELTS',['SECONDARY','HIGH_SCHOOL','ADULT'],'5.5','BASIC',4800000,90],
 ['IL3','IELTS 6.5','IELTS',['HIGH_SCHOOL','ADULT'],'6.5','5.5',5500000,90],
 ['IL4','IELTS 7.0','IELTS',['HIGH_SCHOOL','ADULT'],'7.0','6.5',6500000,90],
 ['TC1','TOEIC nền tảng','TOEIC',['HIGH_SCHOOL','ADULT'],'BASIC',null,2700000,90],
 ['TC2','TOEIC 450','TOEIC',['HIGH_SCHOOL','ADULT'],'450','BASIC',3200000,90],
 ['TC3','TOEIC 650','TOEIC',['HIGH_SCHOOL','ADULT'],'650','450',4000000,90],
 ['TC4','TOEIC 800','TOEIC',['ADULT'],'800','650',4800000,90]
];
const courses=[];
function course(def,branch=null){const [code,name,pc,groups,output,input,fee,minutes]=def,p=plan.programs.find(p=>p.code===pc),out=level(pc,output,output),entry=input?level(pc,input,input):null;
 const c=add('courses',{id:next('courses'),code,name,type:null,level:output,branch_id:branch,tuition_fee:fee,max_students:20,status:'OPEN',duration:null,expected_minutes:minutes*8,duration_revision:1,classification_revision:1,program_id:p.id,content:'8 buổi học; mỗi buổi '+minutes+' phút. Dữ liệu mẫu phục vụ nghiệm thu local.',entry_requirement:entry?'Cần đánh giá đầu vào xác nhận '+entry.name:'Nhập môn; tư vấn và kiểm tra thông tin trước ghi danh'});
 for(const code of groups){const a=plan.audiences.find(a=>a.code===code);assert.ok(a);add('course_audiences',{course_id:c.id,audience_group_id:a.id});}
 add('course_duration_plans',{id:next('course_duration_plans'),course_id:c.id,revision:1,expected_minutes:c.expected_minutes,actor_id:sa.id,actor_name:sa.username,occurred_at:stamp,reason:'Kế hoạch bộ mẫu mới D3'});
 const policy=add('course_admission_policies',{id:next('course_admission_policies'),course_id:c.id,revision:1,mode:entry?'REQUIRES_ASSESSMENT':'OPEN_ENTRY',program_id:p.id,program_code:p.code,program_name:p.name,output_level_id:out.id,output_level_code:out.code,output_level_name:out.name,confirmed_by:sa.id,confirmed_by_name:sa.username,confirmed_at:stamp,reason:'Chính sách bộ mẫu D3, không phải xác nhận dữ liệu cũ'});
 if(entry)add('course_admission_levels',{policy_id:policy.id,level_id:entry.id,code:entry.code,name:entry.name});
 courses.push({...c,p,policy,entry,groups,minutes});return courses.at(-1);
}
for(const def of definitions)course(def);
for(const b of plan.branches)course(['GT-'+b.code,'Giao tiếp '+b.code,'COMMUNICATION',['ADULT'],'BASIC',null,2900000,90],b.id);
const surname=['Nguyễn','Trần','Lê','Phạm','Võ','Đặng','Bùi','Đỗ','Hồ','Ngô','Dương','Lý','Trịnh','Phan','Vũ','Đinh','Mai','Đào','Cao','Tạ'];
const given=['Minh Anh','Quốc Bảo','Thu Hà','Hoàng Nam','Ngọc Linh','Gia Huy','Khánh Vy','Đức Anh','Thanh Tâm','Mai Chi','Tuấn Kiệt','Ngọc Hân','Minh Khang','Bảo Trâm','Nhật Minh','Hải Đăng','Hoài An','Bảo Ngọc','Minh Quân','Thanh Thảo'];
const fullName=i=>surname[i%surname.length]+' '+given[Math.floor(i/surname.length)%given.length];
const pairs=[[[5,'09:00:00'],[6,'09:00:00']],[[5,'10:30:00'],[6,'10:30:00']],[[0,'18:00:00'],[2,'18:00:00']],[[1,'18:00:00'],[3,'18:00:00']],[[0,'19:45:00'],[2,'19:45:00']],[[1,'19:45:00'],[3,'19:45:00']],[[4,'18:00:00'],[5,'14:00:00']],[[4,'18:00:00'],[6,'14:00:00']]];
const endTime=(time,minutes)=>{const [h,m]=time.split(':').map(Number),n=h*60+m+minutes;return String(Math.floor(n/60)).padStart(2,'0')+':'+String(n%60).padStart(2,'0')+':00';};
const allClasses=[],allStudents=[],enrollments=[];
let studentNumber=Number(query('language_center_db',"SELECT GREATEST((SELECT COALESCE(MAX(id),0) FROM student_code_numbers),(SELECT COALESCE(MAX(CAST(SUBSTRING(student_code,3) AS UNSIGNED)),0) FROM students WHERE student_code REGEXP '^HV[0-9]+$'))"));
function enrollment(s,cl,historic=false){const staff=cl.staff,p=cl.course.policy,eid=next('enrollments'),accepted=date(cl.start_date,-3)+' 09:00:00';let assessment=null;
 if(cl.course.entry){const l=cl.course.entry;assessment=add('placement_assessments',{id:next('placement_assessments'),student_id:s.id,branch_id:s.branch_id,program_id:p.program_id,program_code:p.program_code,program_name:p.program_name,level_id:l.id,level_code:l.code,level_name:l.name,method:'INTERVIEW',assessed_on:date(cl.start_date,-4),assessor_name:cl.teacher.full_name,source:'Phỏng vấn đầu vào trong bộ dữ liệu mẫu local',status:'CONFIRMED',confirmed_by:staff.account_id,confirmed_by_name:staff.full_name,confirmed_at:date(cl.start_date,-4)+' 10:00:00',recorded_by:staff.account_id,recorded_by_name:staff.full_name,created_at:date(cl.start_date,-4)+' 09:00:00',updated_at:date(cl.start_date,-4)+' 10:00:00',version:0});
 add('assessment_events',{id:next('assessment_events'),assessment_id:assessment.id,assessment_version:0,action:'CONFIRMED',actor_id:staff.account_id,actor_name:staff.full_name,before_state:'DRAFT',after_state:JSON.stringify({status:'CONFIRMED',programCode:p.program_code,levelCode:l.code}),occurred_at:assessment.confirmed_at,reason:'Xác nhận phỏng vấn dữ liệu mẫu'});}
 const e=add('enrollments',{id:eid,student_id:s.id,class_id:cl.id,enrolled_at:accepted,study_status:'STUDYING',tuition_fee:cl.course.tuition_fee,registered_by:staff.account_id,registered_by_name:staff.full_name,tuition_delivery:'CREATED',version:0});
 add('enrollment_periods',{id:next('enrollment_periods'),enrollment_id:e.id,active_from:cl.start_date,active_until:historic?date(cl.end_date,1):null});
 add('enrollment_admissions',{id:next('enrollment_admissions'),enrollment_id:e.id,student_id:s.id,class_id:cl.id,class_revision:1,policy_id:p.id,academic_status:'ELIGIBLE',intent:'ENROLL',fingerprint:sha('refresh-admission-'+e.id),policy_snapshot:JSON.stringify({mode:p.mode,policyId:p.id,programId:p.program_id,outputLevelCode:p.output_level_code}),assessment_id:assessment?.id,assessment_version:assessment?0:null,assessment_snapshot:assessment?JSON.stringify({id:assessment.id,version:0,programCode:p.program_code,levelCode:assessment.level_code,status:'CONFIRMED'}):'NONE',actor_id:staff.account_id,actor_name:staff.full_name,accepted_at:accepted});
 add('learning_events',{id:next('learning_events'),branch_id:s.branch_id,student_id:s.id,enrollment_id:e.id,action:'ENROLLED',old_status:null,new_status:'STUDYING',reason:historic?'Lớp đã học xong, chờ trung tâm chốt kết quả':'Ghi danh bộ mẫu mới',effective_date:cl.start_date,actor_id:staff.account_id,actor_name:staff.full_name,occurred_at:accepted});
 const fee=cl.course.tuition_fee,paid=historic?fee:[fee,fee/2,0][s.id%3],due=date(cl.start_date,7);
 const tuition=add('tuition',{id:next('tuition'),enrollment_id:e.id,student_id:s.id,student_name:s.full_name,class_id:cl.id,class_name:cl.name,course_id:cl.course.id,course_name:cl.course.name,branch_id:s.branch_id,currency:'VND',original_amount:fee,discount_amount:0,amount:fee,paid_amount:paid,due_date:due,status:paid===fee?'PAID':paid?'PARTIAL':due<today?'OVERDUE':'UNPAID',version:0});
 if(paid)add('payments',{id:next('payments'),tuition_id:tuition.id,branch_id:s.branch_id,amount:paid,status:'PAID',payment_method:s.id%2?'CASH':'BANK_TRANSFER',paid_at:date(cl.start_date,-2)+' 10:00:00',receipt_no:'PT'+String(max.payments).padStart(6,'0'),request_key:'refresh-receipt-'+e.id,recorded_by:staff.account_id,recorded_by_name:staff.full_name,note:'Phiếu thu mẫu local, không phải giao dịch VNPAY'});
 cl.current_size++;const record={...e,student:s,cl,tuition};enrollments.push(record);return record;
}
for(const [bi,b] of plan.branches.entries()){
 const teachers=plan.teachers.filter(t=>t.work_status==='ACTIVE'&&plan.accounts.find(a=>a.id===t.account_id)?.branch_id===b.id).sort((a,b)=>a.id-b.id);assert.ok(teachers.length>=2,'Need two primary teachers per branch');
 const staff=plan.staffs.filter(s=>plan.accounts.find(a=>a.id===s.account_id)?.branch_id===b.id);assert.equal(staff.length,2);
 const rooms=Array.from({length:3},(_,i)=>add('rooms',{id:next('rooms'),branch_id:b.id,code:b.code+'-P'+(i+1),name:'Phòng '+(i+1),capacity:20,status:'AVAILABLE'}));
 const classCourses=['MG1','TH1','CS1','GT1','IL2','TC2','PT1','GT-'+b.code,'IL1','MG2'];
 const classes=[];
 for(let k=0;k<10;k++){const co=courses.find(c=>c.code===classCourses[k]),teacher=teachers[k%2],first=k===8?historical:k===9?upcoming:start,pair=pairs[k===8?4:k===9?0:k],scheduleRows=[];
 const dates=Array.from({length:4},(_,w)=>pair.map(([day,time])=>({date:date(first,w*7+day),time}))).flat().sort((a,b)=>a.date.localeCompare(b.date));
 const c=add('school_classes',{id:next('school_classes'),branch_id:b.id,code:b.code+'-L'+String(k+1).padStart(2,'0'),name:co.name+' '+b.code,course_id:co.id,room_id:rooms[k%2].id,teacher_id:teacher.id,teacher_name:teacher.full_name,max_size:20,current_size:0,start_date:dates[0].date,end_date:dates.at(-1).date,status:k===8?'FINISHED':k===9?'UPCOMING':'ONGOING',status_manually_set:0,sessions_initialized:1,session_end_date:dates.at(-1).date,admission_policy_id:co.policy.id,admission_revision:1,expected_minutes:co.expected_minutes,duration_revision:1});
 add('class_duration_plans',{id:next('class_duration_plans'),class_id:c.id,course_id:co.id,course_revision:1,revision:1,expected_minutes:co.expected_minutes,source:'COURSE',actor_id:sa.id,actor_name:sa.username,occurred_at:date(c.start_date,-5)+' 09:00:00',reason:'Kế hoạch lớp mẫu theo khóa'});
 add('class_admission_events',{id:next('class_admission_events'),class_id:c.id,revision:1,new_policy_id:co.policy.id,actor_id:sa.id,actor_name:sa.username,occurred_at:date(c.start_date,-5)+' 09:00:00',reason:'Chính sách lớp mẫu mới'});
 for(const [day,time] of pair)scheduleRows.push(add('schedules',{id:next('schedules'),class_id:c.id,day_of_week:['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'][day],start_time:time,end_time:endTime(time,co.minutes)}));
 const sessions=dates.map(v=>{const day=(new Date(v.date+'T00:00:00Z').getUTCDay()+6)%7,sc=scheduleRows[pair.findIndex(p=>p[0]===day)];return add('class_sessions',{id:next('class_sessions'),class_id:c.id,session_date:v.date,start_time:v.time,end_time:endTime(v.time,co.minutes),room_id:c.room_id,room_name:rooms[k%2].name,planned_teacher_id:teacher.id,planned_teacher_name:teacher.full_name,teacher_id:teacher.id,teacher_name:teacher.full_name,status:v.date<today?'COMPLETED':'SCHEDULED',kind:'REGULAR',source_date:v.date,source_schedule_id:sc.id,customized:0,version:0,created_at:date(c.start_date,-5)+' 09:00:00',updated_at:v.date<today?v.date+' 23:00:00':date(c.start_date,-5)+' 09:00:00'});});
 classes.push({...c,course:co,teacher,staff:staff[k%2],sessions,current_size:0});
 }
 const accounts=plan.accounts.filter(a=>a.role==='STUDENT'&&a.branch_id===b.id).sort((a,b)=>a.id-b.id);assert.equal(accounts.length,100);
 const students=[];
 for(const [i,account] of accounts.entries()){const cl=classes[i%8],code='HV'+String(++studentNumber).padStart(4,'0'),age=i%8===0?5:i%8===1?9:i%8===2?13:i%8===4?16:22;
 const s=add('students',{id:next('students'),account_id:account.id,branch_id:b.id,student_code:code,full_name:fullName(bi*100+i),date_of_birth:(Number(today.slice(0,4))-age)+'-05-15',gender:i%2?'MALE':'FEMALE',email:code.toLowerCase()+'@example.com',phone:'090'+String(bi*100+i+1).padStart(7,'0'),address:'Địa chỉ mẫu '+b.code,registered_at:registered,status:'STUDYING',guardian_name:age<18?'Phụ huynh '+fullName(bi*100+i):null,guardian_phone:age<18?'091'+String(bi*100+i+1).padStart(7,'0'):null,current_level:null,version:0});
 students.push(s);allStudents.push({...s,username:account.username});enrollment(s,cl);
 }
 // A past cohort with published grades, awaiting an actual final-result confirmation.
 for(const s of students.filter((_,i)=>i%8===4).slice(0,8))enrollment(s,classes[8],true);
 for(const c of classes){update('school_classes',c.id,{current_size:c.current_size});allClasses.push(c);}
 for(let i=0;i<20;i++){const st=staff[i%2],co=courses.filter(c=>c.branch_id===null||c.branch_id===b.id)[i%17],state=['NEW','CONTACTED','CONSULTING','LOST'][i%4],l=add('leads',{id:next('leads'),branch_id:b.id,full_name:fullName(bi*100+i)+' (tư vấn)',phone:'092'+String(bi*100+i+1).padStart(7,'0'),email:'lead'+b.code+i+'@example.com',source:['WEBSITE','REFERRAL','FACEBOOK'][i%3],status:state,interested_course_id:co.id,assigned_to:st.account_id,assigned_to_name:st.full_name,desired_level:co.level,next_contact_at:state==='LOST'?null:date(today,i%3-1)+' 15:00:00',last_contacted_at:state==='NEW'?null:date(today,-2)+' 09:00:00',created_at:date(today,-3)+' 09:00:00',updated_at:now,version:0});
 add('lead_activities',{id:next('lead_activities'),lead_id:l.id,action:'CREATED',note:'Nhu cầu học trong bộ mẫu local',new_status:state,assignee_id:st.account_id,assignee_name:st.full_name,actor_id:st.account_id,actor_name:st.full_name,occurred_at:l.created_at});}
 for(const s of students.slice(0,2)){const r=add('student_support_requests',{id:next('student_support_requests'),branch_id:b.id,student_id:s.id,category:'PROFILE',subject:'Kiểm tra thông tin liên hệ',status:'OPEN',created_at:now,updated_at:now,version:0});add('student_support_messages',{id:next('student_support_messages'),request_id:r.id,actor_id:s.account_id,actor_name:s.full_name,sender:'STUDENT',message:'Nhờ trung tâm kiểm tra thông tin liên hệ của em.',internal_note:0,created_at:now});}
}
add('student_code_numbers',{id:studentNumber});
for(const c of allClasses){const members=enrollments.filter(e=>e.class_id===c.id),completed=c.sessions.filter(s=>s.status==='COMPLETED');
 for(const session of completed)for(const e of members)add('attendances',{id:next('attendances'),class_session_id:session.id,enrollment_id:e.id,session_date:session.session_date,status:e.student_id%13===0?'ABSENT':e.student_id%11===0?'LATE':'PRESENT',has_permission:0,recorded_by:c.teacher.account_id,recorded_by_name:c.teacher.full_name});
 if(!members.length||!completed.length)continue;
 const pc=c.course.p.code,maxScore=pc==='IELTS'?9:pc==='TOEIC'?990:10,examDate=completed.at(-1).session_date,title='Kiểm tra nội bộ '+c.code,type=pc==='IELTS'?'IELTS':pc==='TOEIC'?'TOEIC':'TEST';
 const ex=add('exams',{id:next('exams'),class_id:c.id,branch_id:c.branch_id,name:title,exam_type:type,exam_date:examDate,max_score:maxScore,weight:1,created_at:date(examDate,-1)+' 09:00:00',created_by:c.teacher.account_id,created_by_name:c.teacher.full_name,roster_frozen_at:examDate+' 08:00:00',roster_frozen_by:c.teacher.account_id,roster_frozen_by_name:c.teacher.full_name,version:0});const cs=[],gs=[];
 for(const e of members.sort((a,b)=>a.student_id-b.student_id)){const score=pc==='IELTS'?5+(e.student_id%5)*0.5:pc==='TOEIC'?450+(e.student_id%8)*50:6+(e.student_id%8)*0.5,comment='Nhận xét bài kiểm tra nội bộ';
 add('exam_candidates',{id:next('exam_candidates'),exam_id:ex.id,student_id:e.student_id,student_name:e.student.full_name,enrollment_id:e.id,source:'ENROLLMENT_PERIOD'});
 add('grades',{id:next('grades'),exam_id:ex.id,student_id:e.student_id,student_name:e.student.full_name,branch_id:c.branch_id,score,student_comment:comment,entered_by:c.teacher.account_id,entered_by_name:c.teacher.full_name,entered_at:examDate+' 23:10:00',version:0});cs.push([e.student_id,e.id,'ENROLLMENT_PERIOD']);gs.push([e.student_id,score,comment,0]);}
 const draft=draftFingerprint({name:title,type,date:examDate,maxScore,weight:1,frozen:true,candidates:cs,grades:gs}),pub=add('grade_publications',{id:next('grade_publications'),exam_id:ex.id,revision:1,request_id:uuid('refresh-pub-'+ex.id),fingerprint:sha([ex.id,'PUBLISH',0,draft,'Công bố kiểm tra nội bộ']),draft_fingerprint:draft,action:'PUBLISH',reason:'Công bố kiểm tra nội bộ',published_by:c.teacher.account_id,published_at:examDate+' 23:20:00',class_id:c.id,branch_id:c.branch_id,exam_name:title,exam_type:type,exam_date:examDate,max_score:maxScore,weight:1});
 for(const e of members)add('published_grades',{id:next('published_grades'),publication_id:pub.id,enrollment_id:e.id,student_id:e.student_id,score:gs.find(g=>g[0]===e.student_id)[1],student_comment:'Nhận xét bài kiểm tra nội bộ'});
}
for(const s of allStudents){const e=enrollments.find(e=>e.student_id===s.id),event=uuid('refresh-grades-'+e.id),source='refresh:grades:'+e.id;
 add('student_notification_outbox',{id:event,source_key:source,category:'GRADES',type:'GRADES_PUBLISHED',title:'Điểm kiểm tra đã được công bố',body:'Mở kết quả học tập để xem điểm kiểm tra nội bộ.',student_id:s.id,enrollment_id:e.id,class_id:e.class_id,course_id:e.cl.course.id,branch_id:s.branch_id,resource_type:'GRADES',resource_id:String(e.id),occurred_at:now,delivered_at:now,recipient_count:1,status:'DONE'});
 add('student_notifications',{id:next('student_notifications'),event_id:event,account_id:s.account_id,student_id:s.id,category:'GRADES',type:'GRADES_PUBLISHED',title:'Điểm kiểm tra đã được công bố',body:'Mở kết quả học tập để xem điểm kiểm tra nội bộ.',resource_type:'GRADES',resource_id:String(e.id),created_at:now});}
// Keep credentials, account IDs and memberships, but expire all cached JWT sessions.
sql.push('UPDATE accounts SET session_version=COALESCE(session_version,0)+1,last_login=NULL;');
for(const [table,count] of Object.entries(counts).filter(([t])=>!['program_levels','student_code_numbers'].includes(t)))sql.push('INSERT INTO refresh_assertions VALUES ('+q(table+' expected count')+',IF((SELECT COUNT(*) FROM `'+table+'`)='+count+',1,0));');
// The complete relational/accounting validator is appended before COMMIT by refresh-run.cjs.
fs.writeFileSync(path.join(dir,'seed-body.sql'),sql.join('\n')+'\n');
const manifest={phase:'D3',status:'generated',asOf:today,counts,studentCodeRange:[allStudents[0].student_code,allStudents.at(-1).student_code],newResourceIdsStartAbove:plan.highWater,courses:courses.map(c=>({code:c.code,name:c.name,program:c.p.code,audiences:c.groups,branchId:c.branch_id,minutes:c.expected_minutes,tuition:c.tuition_fee,admission:c.policy.mode})),classes:allClasses.map(c=>({id:c.id,code:c.code,branchId:c.branch_id,courseCode:c.course.code,status:c.status,students:c.current_size})),pendingFinalResults:'Past classes contain learners awaiting real result confirmation; no fake graduation/final-result snapshots.',studentLinks:allStudents.map(s=>({accountId:s.account_id,username:s.username,studentId:s.id,studentCode:s.student_code,branchId:s.branch_id}))};
fs.writeFileSync(path.join(dir,'manifest.json'),JSON.stringify(manifest,null,2));
fs.writeFileSync(path.join(root,'docs/course-classification/d3-seed-manifest.json'),JSON.stringify({...manifest,studentLinks:undefined,newResourceIdsStartAbove:undefined},null,2));
console.log(JSON.stringify({phase:'D3',status:'generated',asOf:today,counts,studentCodeRange:manifest.studentCodeRange}));
