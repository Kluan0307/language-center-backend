const c=require('./data-common.cjs'),{checks}=require('../identity/refresh-validation.cjs');
const fs=require('fs'),path=require('path');
const keepAlive=setInterval(()=>{},1000);
(async()=>{
 const relational=[];
 for(const test of checks().filter(t=>!['Website stays private until D5','No jobs queued during refresh'].includes(t.name))){const actual=Number(c.mysql(test.sql));relational.push({name:test.name,expected:test.expected,actual,passed:actual===test.expected});}
 const queries=[
  ['student-enrollments','SELECT id FROM enrollments WHERE student_id=406 ORDER BY id DESC LIMIT 20'],
  ['branch-students','SELECT id FROM students WHERE branch_id=1 ORDER BY id DESC LIMIT 20'],
  ['lead-followup',"SELECT id FROM leads WHERE branch_id=1 AND status='NEW' ORDER BY next_contact_at LIMIT 20"],
  ['student-tuition','SELECT id FROM tuition WHERE student_id=406 ORDER BY due_date,id LIMIT 20']
 ];
 const plans=queries.map(([name,sql])=>({name,plan:JSON.parse(c.mysql('EXPLAIN FORMAT=JSON '+sql))}));
 const result={checkedAt:new Date().toISOString(),status:relational.every(t=>t.passed)?'passed':'findings',relational,plans,sourceDatabaseWritten:false};
 fs.writeFileSync(path.join(c.root,'docs/docker/opt1-opt2-audit.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({status:result.status,checks:relational.length,failures:relational.filter(t=>!t.passed),plans}));
})().catch(e=>{console.error(e.message.split('\n')[0]);process.exitCode=1;}).finally(()=>clearInterval(keepAlive));

