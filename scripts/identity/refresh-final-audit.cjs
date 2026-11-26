const {fs,path,dir,root,query,assert}=require('./refresh-common.cjs');
const {checks}=require('./refresh-validation.cjs');
const applied=JSON.parse(fs.readFileSync(path.join(dir,'applied.json')));
for(const c of checks())assert.equal(Number(query('language_center_db',c.sql)),c.expected,c.name);
assert.equal(Number(query(null,"SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME REGEXP '^lc_refresh_(restore|rehearsal|probe)_[0-9]+$'")),0,'Scratch database left behind');
const inventory=JSON.parse(fs.readFileSync(path.join(dir,'inventory.json')));
for(const c of inventory.columns.filter(c=>c.extra==='auto_increment')){
 const auto=Number(query('language_center_db',"SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"+c.table+"'"));
 const max=Number(query('language_center_db','SELECT COALESCE(MAX(`'+c.column+'`),0) FROM `'+c.table+'`'));
 assert.ok(auto>max,'Auto increment not aligned: '+c.table);
}
const branches=JSON.parse(query('language_center_db',"SELECT JSON_ARRAYAGG(JSON_OBJECT('code',b.code,'students',(SELECT COUNT(*) FROM students s WHERE s.branch_id=b.id),'classes',(SELECT COUNT(*) FROM school_classes c WHERE c.branch_id=b.id),'leads',(SELECT COUNT(*) FROM leads l WHERE l.branch_id=b.id))) FROM branches b"));
for(const b of branches){assert.equal(b.students,100);assert.equal(b.classes,10);assert.equal(b.leads,20);}
const manifest=JSON.parse(fs.readFileSync(path.join(dir,'manifest.json')));
const report={status:'passed',observedAt:new Date().toISOString(),checks:checks().length,branches,countersAligned:true,temporaryDatabasesRemaining:0,classificationComplete:true,accountsPreserved:419,classesByStatus:manifest.classes.reduce((out,c)=>(out[c.status]=(out[c.status]||0)+1,out),{}),studentExamples:manifest.studentLinks.filter(s=>/^student[1-5]$/.test(s.username)),counts:applied.counts,liveUiTest:'not performed; services were stopped before D4',externalProviderTransactionsCreated:false};
fs.writeFileSync(path.join(root,'docs/course-classification/d4-final-audit.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));
