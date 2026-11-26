import fs from 'node:fs';
const corpus=JSON.parse(fs.readFileSync('docs/assistant/readonly-scenarios.json','utf8'));
// Hand-reviewed expected intents. Never derive an expected answer from TopicPolicy/model output.
const families={
SUPER_ADMIN:'PERSONNEL_STATS PERSONNEL_STATS PERSONNEL PERSONNEL PERSONNEL_TEACHING PERSONNEL CENTER_STATS CENTER_STATS ACCOUNTS PERSONNEL_STATS ACCOUNTS ACCOUNT_STATS COURSES CLASSES ACADEMIC_STATS ACADEMIC_STATS RESOURCES AVAILABLE_TEACHERS DEBT_STATS DEBT_STATS CASH_FLOW CASH_FLOW CASH_COMPARE CARE_STATS',
ADMIN:'PERSONNEL_STATS PERSONNEL PERSONNEL PERSONNEL PERSONNEL_TEACHING PERSONNEL_STATS ROSTER_STATS CLASSES CLASSES CLASSES SCHEDULE RESOURCES STUDENT_CLASSES STUDENTS STATS ABSENCES MISSING_ATTENDANCE EXAMS TUITION DEBT_STATS RECEIPTS LEADS CARE_STATS SUPPORT_STATS',
STAFF:'COURSES COURSES ADMISSIONS CLASSES CLASSES SCHEDULE STUDENTS STUDENT_CLASSES SELECTION SCHEDULE CLASSES PERSONNEL DEBT_STATS TUITION TUITION RECEIPTS RECEIPTS HELP LEADS LEADS LEADS LEADS SUPPORT RESULTS',
TEACHER:'PERSONNEL_SELF PERSONNEL_SELF PERSONNEL_SELF PERSONNEL_SELF PERSONNEL_TEACHING PERSONNEL_TEACHING SCHEDULE SCHEDULE SCHEDULE SCHEDULE SCHEDULE SCHEDULE ROSTER_STATS ROSTER_STATS STUDENTS ATTENDANCE ATTENDANCE ABSENCES RESULTS RESULTS RESULTS RESULTS EXAMS MISSING_ATTENDANCE',
};
const day=new Intl.DateTimeFormat('sv-SE',{timeZone:'Asia/Ho_Chi_Minh'}).format(new Date());
const add=n=>new Date(Date.parse(day+'T00:00:00Z')+n*86400000).toISOString().slice(0,10);
const weekday=new Date(day+'T00:00:00Z').getUTCDay();const monday=add(-((weekday+6)%7));const sunday=new Date(Date.parse(monday)+6*86400000).toISOString().slice(0,10);
const period={from:'2026-11-01',to:'2026-11-10'};
const slots={teacherName:'An',staffName:'An',adminName:'An',studentName:'An',studentCode:'HV11',classCode:'L1',leadName:'An',date:add(2),startTime:'18:00',endTime:'20:00',fromDate:period.from,toDate:period.to,sessionId:'101',examId:'501',enrollmentId:'301',supportId:'1',level:'A2'};
const call=(tool,args={})=>({tool,args});
function base(family){switch(family){
case 'PERSONNEL_STATS':return [call('get_personnel_statistics',{kind:'TEACHER'})];
case 'PERSONNEL':return [call('find_personnel',{keyword:'An'}),call('get_personnel_profile',{personnelId:11})];
case 'PERSONNEL_SELF':return [call('get_my_personnel_profile')];
case 'PERSONNEL_TEACHING':return [call('find_personnel',{keyword:'An'}),call('get_teacher_classes',{teacherId:11})];
case 'CENTER_STATS':return [call('get_center_statistics')];
case 'ACCOUNTS':return [call('find_management_accounts')];
case 'ACCOUNT_STATS':return [call('get_account_statistics')];
case 'COURSES':return [call('search_courses',{keyword:'Giao tiếp',maxTuition:5000000,maxInclusive:false})];
case 'CLASSES':return [call('search_classes')];
case 'ACADEMIC_STATS':return [call('get_academic_statistics')];
case 'RESOURCES':return [call('search_rooms',{from:slots.date,startTime:slots.startTime,endTime:slots.endTime})];
case 'AVAILABLE_TEACHERS':return [call('find_available_teachers',{from:slots.date,startTime:slots.startTime,endTime:slots.endTime})];
case 'DEBT_STATS':return [call('get_debt_statistics')];
case 'CASH_FLOW':return [call('get_cash_flow',period)];
case 'CASH_COMPARE':return [];
case 'CARE_STATS':return [call('get_care_statistics')];
case 'ROSTER_STATS':return [call('get_roster_statistics')];
case 'SCHEDULE':return [call('list_sessions',{from:monday,to:sunday})];
case 'STUDENT_CLASSES':return [call('find_students',{keyword:'An'}),call('get_student_classes',{studentId:11})];
case 'STUDENTS':return [call('find_students',{keyword:'An'})];
case 'STATS':return [call('get_student_statistics')];
case 'ABSENCES':return [call('get_attendance_summary',{from:add(-29),to:day,minAbsences:3})];
case 'MISSING_ATTENDANCE':return [call('list_missing_attendance',{from:add(-29),to:day})];
case 'EXAMS':return [call('list_learning_exams',{missingOnly:true})];
case 'TUITION':return [call('search_tuitions',{overdue:true})];
case 'RECEIPTS':return [call('search_receipts')];
case 'LEADS':return [call('search_leads')];
case 'SUPPORT_STATS':return [call('get_support_statistics')];
case 'SUPPORT':return [call('search_support_requests',{supportId:1})];
case 'ADMISSIONS':return [call('search_course_admissions',{entryLevel:'A2'})];
case 'HELP':return [];
case 'RESULTS':return [call('get_learning_results',{view:'CURRENT',examId:501})];
default:return [];
}}
const cases=corpus.cases.map(c=>{
 const s={id:c.id,role:c.role,prompt:c.prompt.replace(/\{(\w+)\}/g,(_,k)=>slots[k]??k),expectedStatus:'ANSWER',scopeMode:c.role==='SUPER_ADMIN'?'WHOLE_SYSTEM':'ACTIVE_BRANCH',plan:[],setupPlan:[],setupPrompt:null,layer:'HOST_SCRIPTED_MCP'};
 if(c.role==='ROLE_MATRIX'){s.behavior=c.id;s.actors=c.id==='FOLLOWUP-11'?['TEACHER#11','TEACHER#12']:/GUARD-0[123]/.test(c.id)?['TEACHER']:/GUARD-0[45]/.test(c.id)?['STAFF']:['ADMIN'];return s;}
 s.expectedFamily=families[c.role].split(' ')[Number(c.id.slice(-2))-1];s.plan=base(s.expectedFamily);
 if(c.role==='SUPER_ADMIN'&&/chi nhánh 1/i.test(c.prompt))s.scopeMode='ACTIVE_BRANCH';
 if(c.id==='ADMIN-06')s.scopeMode='ALL_ASSIGNED';
 if(c.id==='SUPER_ADMIN-06'){s.plan=[call('assistant_decision',{action:'CLARIFY',reason:'REPHRASE'})];s.expectedStatus='CLARIFICATION_REQUIRED';}
 if(c.id==='SUPER_ADMIN-10'||c.id==='ADMIN-06')s.plan=[call('get_personnel_statistics',{kind:'STAFF'})];
 if(c.id==='SUPER_ADMIN-13')s.plan=[call('search_courses',{keyword:'IELTS'})];
 if(c.id==='ADMIN-02')s.plan=[call('find_personnel',{keyword:'An'}),call('assistant_decision',{action:'DONE'})];
 if(['ADMIN-03','ADMIN-04','ADMIN-05'].includes(c.id)){s.setupPrompt='Giáo viên An có bằng cấp gì?';s.setupPlan=base('PERSONNEL');s.plan=c.id==='ADMIN-05'?[call('get_teacher_classes',{teacherId:11})]:[call('get_personnel_profile',{personnelId:11})];}
 if(c.id==='SUPER_ADMIN-22'){s.setupPrompt='Thực thu từ 2026-11-01 đến 2026-11-10?';s.setupPlan=base('CASH_FLOW');}
 if(c.id==='SUPER_ADMIN-23'){s.plan=[];s.expectedStatus='CLARIFICATION_REQUIRED';}
 if(c.id==='SUPER_ADMIN-18'){s.plan=[];s.expectedStatus='CLARIFICATION_REQUIRED';s.limitation='Prompt does not specify a date/time window';}
 if(c.id==='ADMIN-10'){s.plan=[call('assistant_decision',{action:'UNSUPPORTED_CAPABILITY'})];s.expectedStatus='UNSUPPORTED_CAPABILITY';s.limitation='No actual class closure timestamp';}
 if(c.id==='ADMIN-18'){s.plan=[];s.expectedStatus='UNSUPPORTED_CAPABILITY';s.limitation='No grading deadline';}
 if(c.id==='STAFF-12'){s.plan=[];s.expectedError='FORBIDDEN';s.expectedStatus=null;}
 if(c.id==='ADMIN-11')s.plan=[call('find_personnel',{keyword:'An'}),call('list_sessions',{teacherId:11,from:monday,to:sunday})];
 if(c.id==='ADMIN-12')s.plan=[call('search_rooms',{status:'MAINTENANCE'})];
 if(c.id==='ADMIN-07'||c.id==='TEACHER-13')s.plan=[call('search_classes',{keyword:'L1'}),call('get_roster_statistics',{classId:9})];
 if(c.id==='ADMIN-21'){s.setupPrompt='Phiếu thu từ 2026-11-01 đến 2026-11-10?';s.setupPlan=[call('search_receipts',period)];s.plan=[call('search_receipts',{receiptState:'VOIDED',...period})];}
 if(c.id==='ADMIN-23')s.plan=[call('find_management_accounts',{keyword:'An',accountRole:'STAFF'}),call('get_care_statistics',{assigneeId:101})];
 if(c.id==='STAFF-02'){s.setupPrompt='Khóa giao tiếp dưới 5 triệu?';s.setupPlan=base('COURSES');s.plan=[call('search_courses',{maxTuition:3000000})];s.expectedStatus='NO_RESULTS';}
 if(c.id==='STAFF-06'){s.setupPrompt='Lớp L1 còn chỗ không?';s.setupPlan=[call('search_classes',{keyword:'L1'})];s.plan=[call('list_sessions',{classId:9,from:slots.date,to:slots.date})];}
 if(c.id==='STAFF-09'){s.behavior='DUPLICATE_STUDENT';s.expectedFamily=null;}
 if(c.id==='STAFF-10'){s.setupPrompt='Học viên An đang học lớp nào?';s.setupPlan=base('STUDENT_CLASSES');s.plan=[call('get_student_classes',{studentId:11}),call('list_sessions',{classId:9,from:monday,to:sunday})];}
 if(c.id==='STAFF-13')s.plan=[call('find_students',{keyword:'HV11'}),call('get_debt_statistics',{studentId:11})];
 if(c.id==='STAFF-15')s.plan=[call('search_tuitions',{dueFrom:day,dueTo:add(7),outstandingOnly:true})];
 if(c.id==='STAFF-16'){s.setupPrompt='Tìm học viên An';s.setupPlan=base('STUDENTS');s.plan=[call('search_receipts',{studentId:11})];}
 if(c.id==='STAFF-17'){s.setupPrompt='Lịch sử phiếu thu?';s.setupPlan=base('RECEIPTS');s.plan=[call('search_receipts',{receiptId:51})];}
 if(c.id==='STAFF-19')s.plan=[call('list_priority_leads',{limit:5})];
 if(c.id==='STAFF-24'){s.plan=[call('find_students',{keyword:'HV11'}),call('get_learning_results',{view:'PUBLISHED',studentId:11,enrollmentId:301})];}
 if(c.role==='TEACHER'&&s.expectedFamily==='PERSONNEL_TEACHING')s.plan=[call('get_my_personnel_profile'),call('get_teacher_classes',{teacherId:11})];
 if(c.id==='TEACHER-08')s.plan=[call('list_sessions',{from:add(1),to:add(1)})];
 if(['TEACHER-16','TEACHER-20','TEACHER-21','TEACHER-22'].includes(c.id)){s.setupPrompt=c.id==='TEACHER-16'?'Tìm học viên An':'Điểm bài kiểm tra #501?';s.setupPlan=c.id==='TEACHER-16'?base('STUDENTS'):base('RESULTS');}
 if(c.id==='TEACHER-16')s.plan=[call('get_session_attendance',{sessionId:101,studentId:11})];
 if(c.id==='TEACHER-17')s.plan=[call('get_session_attendance',{sessionId:101,missingOnly:true})];
 if(c.id==='SUPER_ADMIN-03'||c.id==='SUPER_ADMIN-04')s.expectedArguments={personnelId:'11'};
 if(c.id==='ADMIN-14')s.expectedArguments={relationship:'RESERVED'};
 if(c.id==='STAFF-15')s.expectedArguments={outstandingOnly:'true'};
 if(c.id==='TEACHER-09')s.expectedArguments={substituteOnly:'true'};
 if(c.id==='TEACHER-10')s.expectedArguments={kind:'MAKEUP'};
 if(c.id==='TEACHER-20')s.expectedArguments={scoreFilter:'ZERO'};
 if(c.id==='TEACHER-21')s.expectedArguments={scoreFilter:'NOT_GRADED'};
 if(c.id==='SUPER_ADMIN-02')s.expectedArguments={activeOnly:'true'};
 const lastData=s.plan.filter(x=>x.tool!=='assistant_decision').at(-1);
 s.expectedArguments={...lastData?.args,...s.expectedArguments};
 if(c.id==='SUPER_ADMIN-09')s.expectedArguments.accountRole='ADMIN';
 if(c.id==='SUPER_ADMIN-11')s.expectedArguments.enabled='false';
 if(c.id==='SUPER_ADMIN-14'||c.id==='STAFF-04')Object.assign(s.expectedArguments,{recruitingOnly:'true',status:'UPCOMING'});
 if(c.id==='SUPER_ADMIN-20'||c.id==='ADMIN-20')s.expectedArguments.overdue='true';
 if(c.id==='ADMIN-06')s.expectedArguments.activeOnly='true';
 if(c.id==='SUPER_ADMIN-15')s.expectedArguments.metric='CLASSES_ONGOING';
 if(c.id==='SUPER_ADMIN-16')s.expectedArguments.metric='ACTIVE_STUDENTS';
 if(c.id==='TEACHER-12')Object.assign(s.expectedArguments,{upcomingOnly:'true',size:'1'});
 s.expectedTool=s.plan.filter(x=>x.tool!=='assistant_decision').at(-1)?.tool??null;
 return s;
});
fs.writeFileSync('docs/assistant/acceptance-oracles.json',JSON.stringify({version:'R8-1',date:day,layer:'Synthetic host/MCP with manually reviewed intent and tool oracles. NOT real model evaluation.',cases},null,2)+'\n');
console.log(`Prepared ${cases.length} independent acceptance oracles.`);
