const c=require('./data-common.cjs');
const before=c.dockerSnapshot();
const columns=new Set(c.mysql('SELECT CONCAT(table_name,\'.\',column_name) FROM information_schema.columns WHERE table_schema=DATABASE()').trim().split(/\r?\n/));
const sql=c.fs.readFileSync(c.path.join(c.root,'docs/migrations/V031__grading_outcomes_and_releases.sql'),'utf8').replace(/^--.*$/gm,'');
for(const statement of sql.split(';').map(s=>s.trim()).filter(Boolean)){const column=statement.match(/^ALTER TABLE (\w+) ADD COLUMN (\w+)/i);if(column&&columns.has(column[1]+'.'+column[2]))continue;c.mysql(statement+';');}
const after=c.dockerSnapshot();
// Old score rows must remain byte-for-byte identical; new nullable columns in
// grading tables are verified separately by selecting their previous columns.
for(const t of before.tables){if(['grading_marks','grading_activities'].includes(t.name)){
 const current=c.mysql('SELECT JSON_ARRAY('+t.columns.map(c.identifier).join(',')+') FROM '+c.identifier(t.name)).trim().split(/\r?\n/).filter(Boolean);c.assert.equal(c.sha(current.sort().join('\n')),t.sha256);
}else c.assert.equal(after.tables.find(n=>n.name===t.name).sha256,t.sha256,t.name+' data changed');}
console.log(JSON.stringify({status:'passed',tables:after.tables.length,oldDataUnchanged:true}));
