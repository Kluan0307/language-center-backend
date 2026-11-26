// Read-only audit after D5; D4's no-publication assertion intentionally no longer applies.
const {fs,path,root,query,assert}=require('../identity/refresh-common.cjs');
const {checks}=require('../identity/refresh-validation.cjs');
const {fromDatabase}=require('../identity/grade-fingerprints.cjs');
const list=checks().filter(c=>c.name!=='Website stays private until D5');
for(const c of list)assert.equal(Number(query('language_center_db',c.sql)),c.expected,c.name);
const publications=JSON.parse(query('language_center_db',"SELECT JSON_ARRAYAGG(JSON_OBJECT('kind',kind,'count',n)) FROM (SELECT kind,COUNT(*) n FROM website_publications WHERE state='PUBLISHED' GROUP BY kind) p"));
const counts=Object.fromEntries(publications.map(p=>[p.kind,p.count]));assert.deepEqual(counts,{BRANCH:4,CLASS:4,COURSE:20,PROGRAM:4});
assert.equal(Number(query('language_center_db',"SELECT COUNT(*) FROM website_publications p JOIN school_classes c ON c.id=p.source_id WHERE p.kind='CLASS' AND p.state='PUBLISHED' AND (c.status<>'UPCOMING' OR c.start_date<=CURRENT_DATE() OR c.current_size>=c.max_size)")),0,'Only recruiting classes should be public');
assert.equal(Number(query('language_center_db',"SELECT COUNT(*) FROM website_teacher_profiles WHERE state='PUBLISHED' AND show_qualification=1")),0);
const teachers=Number(query('language_center_db',"SELECT COUNT(*) FROM website_teacher_profiles WHERE state='PUBLISHED'"));assert.equal(teachers,8);
const pubs=JSON.parse(query('language_center_db',"SELECT JSON_ARRAYAGG(JSON_OBJECT('exam',exam_id,'draft',draft_fingerprint)) FROM grade_publications WHERE action='PUBLISH'"));
for(const p of pubs)assert.equal(fromDatabase('language_center_db',query,p.exam),p.draft,'Published grade fingerprint '+p.exam);
assert.equal(Number(query('language_center_db',"SELECT COUNT(*) FROM leads WHERE phone IN ('0987000001','0987000002')")),0,'Acceptance leads must stay on the private copy');
const report={status:'passed',observedAt:new Date().toISOString(),readOnly:true,relationalChecks:list.length,gradeFingerprintsVerified:pubs.length,publications:counts,teacherProfiles:teachers,unreviewedQualificationsPublic:false,acceptanceLeadsInLiveDatabase:0,externalTransactionsSeeded:false,temporaryAcceptanceDatabases:Number(query(null,"SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME REGEXP '^lc_identity_http_[0-9]+$'"))};
fs.writeFileSync(path.join(root,'docs/course-classification/d8-final-audit.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));
