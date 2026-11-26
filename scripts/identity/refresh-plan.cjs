const {fs,path,dir,root,read,query,keep,extend,snapshot,assert}=require('./refresh-common.cjs');
const inv=JSON.parse(fs.readFileSync(path.join(dir,'inventory.json')));
const accounts=read('accounts',['id','username','role','branch_id','session_version']);
const students=read('students',['id','account_id','branch_id']);
const branches=read('branches',['id','code','name']).sort((a,b)=>a.code.localeCompare(b.code));
assert.equal(branches.length,4);assert.equal(accounts.filter(a=>a.role==='STUDENT').length,400);
for(const a of accounts.filter(a=>a.role==='STUDENT'))assert.equal(students.filter(s=>s.account_id===a.id&&s.branch_id===a.branch_id).length,1,'Student account mapping '+a.id);
const remaining=new Set(inv.tables.map(t=>t.name).filter(t=>![...keep,...extend].includes(t)));
const reset=[];while(remaining.size){const ready=[...remaining].filter(parent=>!inv.fks.some(f=>f.parent===parent&&f.table!==parent&&remaining.has(f.table)));assert.ok(ready.length,'FK cycle; manual review required');for(const t of ready.sort()){reset.push(t);remaining.delete(t);}}
for(const f of inv.fks)assert.ok(!(keep.includes(f.table)&&reset.includes(f.parent)),'Preserved table depends on reset table: '+f.table);
const baseline=snapshot('language_center_db');
const plan={createdAt:new Date().toISOString(),database:'language_center_db',keep,extend,reset,tables:inv.tables.map(t=>({...t,action:keep.includes(t.name)?'KEEP':extend.includes(t.name)?'EXTEND':'RESET'})),baseline,accounts,students,branches,
 teachers:read('teachers',['id','account_id','full_name','hire_date','work_status']),staffs:read('staffs',['id','account_id','full_name']),memberships:read('account_branches',['account_id','branch_id']),
 programs:read('programs',['id','code','name']),levels:read('program_levels',['id','program_id','code','name']),audiences:read('audience_groups',['id','code','name'])};
plan.highWater={};for(const t of inv.tables){if(inv.columns.some(c=>c.table===t.name&&c.column==='id'&&c.type==='bigint'))plan.highWater[t.name]=Number(query('language_center_db','SELECT COALESCE(MAX(id),0) FROM `'+t.name+'`'));}
fs.writeFileSync(path.join(dir,'plan.json'),JSON.stringify(plan,null,2));
const publicPlan={phase:'D1',status:'complete',database:plan.database,tables:plan.tables,keptAccounts:accounts.length,studentAccountsToRelink:400,branches:branches.map(b=>({code:b.code,name:b.name})),target:{courses:20,classes:40,students:400,leads:80},identityPolicy:'Keep credentials, roles, branch memberships and personnel. Invalidate sessions; map each student account by account_id. New resource IDs exceed previous maxima.'};
fs.writeFileSync(path.join(root,'docs/course-classification/d1-data-plan.json'),JSON.stringify(publicPlan,null,2));
console.log(JSON.stringify({phase:'D1',status:'complete',tables:inv.tables.length,keptTables:keep.length,extendedTables:extend.length,resetTables:reset.length,accounts:accounts.length,studentAccounts:400}));
