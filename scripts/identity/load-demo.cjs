const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const {query,schema,root}=require('./database.cjs');process.chdir(root);
const migration=fs.readFileSync('docs/migrations/V020__employment_lifecycle.sql','utf8');
const fixture=fs.readFileSync('docs/demo/seed-identity-full.sql','utf8');
const scratch='lc_identity_g456_'+Date.now();assert.match(scratch,/^lc_identity_g456_\d+$/);
const checks=[];
function verify(db){
 const zero=(name,sql)=>{assert.equal(Number(query(db,sql)),0,name);checks.push({database:db,name,status:'PASS'});};
 assert.equal(query(db,'SELECT COUNT(*) FROM students'),'30');assert.equal(query(db,'SELECT COUNT(*) FROM accounts'),'41');
 zero('student account role and link',"SELECT COUNT(*) FROM students s LEFT JOIN accounts a ON a.id=s.account_id WHERE a.id IS NULL OR a.role<>'STUDENT' OR a.branch_id<>s.branch_id");
 zero('personnel role and profile',"SELECT COUNT(*) FROM accounts a LEFT JOIN admins ad ON ad.account_id=a.id LEFT JOIN staffs st ON st.account_id=a.id LEFT JOIN teachers t ON t.account_id=a.id WHERE (a.role='ADMIN' AND ad.id IS NULL) OR (a.role='STAFF' AND st.id IS NULL) OR (a.role='TEACHER' AND t.id IS NULL)");
 zero('minor guardian complete',"SELECT COUNT(*) FROM students WHERE date_of_birth>DATE_SUB(CURRENT_DATE,INTERVAL 18 YEAR) AND (guardian_name IS NULL OR guardian_phone IS NULL)");
 zero('class branch and teacher assignment',"SELECT COUNT(*) FROM school_classes c JOIN rooms r ON r.id=c.room_id JOIN courses co ON co.id=c.course_id JOIN teachers t ON t.id=c.teacher_id WHERE r.branch_id<>c.branch_id OR co.branch_id<>c.branch_id OR t.work_status<>'ACTIVE' OR c.start_date<t.hire_date OR NOT EXISTS(SELECT 1 FROM account_branches b WHERE b.account_id=t.account_id AND b.branch_id=c.branch_id AND b.status=1)");
 zero('teacher and room schedule conflicts',"SELECT COUNT(*) FROM class_sessions a JOIN class_sessions b ON a.id<b.id AND a.session_date=b.session_date AND a.start_time<b.end_time AND b.start_time<a.end_time AND (a.teacher_id=b.teacher_id OR a.room_id=b.room_id) WHERE a.status<>'CANCELLED' AND b.status<>'CANCELLED'");
 zero('enrollment branch and accounting root',"SELECT COUNT(*) FROM enrollments e JOIN students s ON s.id=e.student_id JOIN school_classes c ON c.id=e.class_id LEFT JOIN tuition t ON t.enrollment_id=e.id WHERE s.branch_id<>c.branch_id OR t.id IS NULL OR t.student_id<>s.id OR t.class_id<>c.id OR e.tuition_delivery<>'CREATED'");
 zero('cached class size',"SELECT COUNT(*) FROM school_classes c WHERE c.current_size<>(SELECT COUNT(*) FROM enrollments e WHERE e.class_id=c.id AND e.study_status='STUDYING') OR c.current_size>c.max_size");
 zero('tuition balance matches cash receipts',"SELECT COUNT(*) FROM tuition t WHERE t.paid_amount<>(SELECT COALESCE(SUM(p.amount),0) FROM payments p WHERE p.tuition_id=t.id AND p.status='PAID') OR t.paid_amount>t.amount OR t.amount<0");
 zero('receipts recorded by assigned staff',"SELECT COUNT(*) FROM payments p WHERE NOT EXISTS(SELECT 1 FROM accounts a JOIN account_branches b ON b.account_id=a.id WHERE a.id=p.recorded_by AND a.role='STAFF' AND b.status=1 AND b.branch_id=p.branch_id)");
 zero('attendance inside enrollment and session',"SELECT COUNT(*) FROM attendances a JOIN enrollments e ON e.id=a.enrollment_id JOIN class_sessions s ON s.id=a.class_session_id WHERE e.class_id<>s.class_id OR a.session_date<>s.session_date OR s.status<>'COMPLETED' OR NOT EXISTS(SELECT 1 FROM enrollment_periods ep WHERE ep.enrollment_id=e.id AND ep.active_from<=a.session_date AND (ep.active_until IS NULL OR ep.active_until>=a.session_date))");
 zero('exams grades candidates and published snapshots',"SELECT COUNT(*) FROM grades g JOIN exams ex ON ex.id=g.exam_id LEFT JOIN exam_candidates c ON c.exam_id=ex.id AND c.student_id=g.student_id LEFT JOIN enrollments e ON e.id=c.enrollment_id WHERE c.id IS NULL OR e.student_id<>g.student_id OR e.class_id<>ex.class_id OR g.score>ex.max_score OR g.score<0");
 zero('published grade source matches',"SELECT COUNT(*) FROM published_grades p JOIN grade_publications pub ON pub.id=p.publication_id LEFT JOIN grades g ON g.exam_id=pub.exam_id AND g.student_id=p.student_id WHERE g.id IS NULL OR p.score<>g.score");
 zero('planned duration is eight sessions',"SELECT COUNT(*) FROM school_classes c WHERE c.expected_minutes<>(SELECT SUM(TIMESTAMPDIFF(MINUTE,s.start_time,s.end_time)) FROM class_sessions s WHERE s.class_id=c.id AND s.status<>'CANCELLED')");
 zero('admission decision links',"SELECT COUNT(*) FROM enrollments e LEFT JOIN enrollment_admissions a ON a.enrollment_id=e.id LEFT JOIN school_classes c ON c.id=e.class_id WHERE a.id IS NULL OR a.class_id<>e.class_id OR a.student_id<>e.student_id OR a.policy_id<>c.admission_policy_id OR a.academic_status<>'ELIGIBLE'");
 assert.equal(query(db,'SELECT MAX(id) FROM student_code_numbers'),'30');assert.equal(query(db,'SELECT MAX(id) FROM personnel_code_numbers'),'10');
 return {students:30,accounts:41,classes:Number(query(db,'SELECT COUNT(*) FROM school_classes')),sessions:Number(query(db,'SELECT COUNT(*) FROM class_sessions')),receipts:Number(query(db,'SELECT COUNT(*) FROM payments'))};
}
query(null,'CREATE DATABASE `'+scratch+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;');
try{
 const ddl=schema();assert.ok(!/^USE\s|^CREATE DATABASE/im.test(ddl));query(scratch,ddl);query(scratch,migration);query(scratch,migration);query(scratch,fixture);
 const counts=verify(scratch);
 query(scratch,"UPDATE teachers SET work_status='WORKING' WHERE id=1; UPDATE staffs SET work_status='INACTIVE',end_date=NULL WHERE id=2;");
 query(scratch,migration);assert.equal(query(scratch,'SELECT work_status FROM teachers WHERE id=1'),'ACTIVE');assert.equal(query(scratch,'SELECT end_date IS NULL FROM staffs WHERE id=2'),'1');
 const result={date:new Date().toISOString(),migrationReplay:'PASS',unknownHistoricalDatePreserved:true,scratch,counts,checks};
 if(process.argv.includes('--apply-local')){
  const tables=query('language_center_db',"SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE'").split(/\r?\n/);
  for(const table of tables)assert.equal(Number(query('language_center_db','SELECT COUNT(*) FROM `'+table.replace(/`/g,'``')+'`')),0,'Refusing to overwrite nonempty '+table);
  query('language_center_db',migration);query('language_center_db',fixture);verify('language_center_db');result.localDatabase='language_center_db';result.localSeed='APPLIED';
 }
 if(process.argv.includes('--verify-local')){verify('language_center_db');result.localDatabase='language_center_db';result.localVerification='PASS';}
 fs.writeFileSync('storage/identity-g456-demo-verification.json',JSON.stringify(result,null,2));console.log(JSON.stringify({status:'PASS',...counts,localSeed:result.localSeed??'NOT_REQUESTED',localVerification:result.localVerification,checks:checks.length}));
}finally{query(null,'DROP DATABASE `'+scratch+'`;');}
