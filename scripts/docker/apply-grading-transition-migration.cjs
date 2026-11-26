const c=require('./data-common.cjs');const before=c.dockerSnapshot();
const columns=new Set(c.mysql("SELECT CONCAT(table_name,'.',column_name) FROM information_schema.columns WHERE table_schema=DATABASE()").trim().split(/\r?\n/));
const sql=c.fs.readFileSync(c.path.join(c.root,'docs/migrations/V032__grading_transition.sql'),'utf8').replace(/^--.*$/gm,'');
for(const statement of sql.split(';').map(s=>s.trim()).filter(Boolean)){const col=statement.match(/^ALTER TABLE (\w+) ADD COLUMN (\w+)/i);if(col&&columns.has(col[1]+'.'+col[2]))continue;c.mysql(statement+';');}
const after=c.dockerSnapshot();for(const t of before.tables){if(['final_results','grading_activities'].includes(t.name)){const rows=c.mysql('SELECT JSON_ARRAY('+t.columns.map(c.identifier).join(',')+') FROM '+c.identifier(t.name)).trim().split(/\r?\n/).filter(Boolean);c.assert.equal(c.sha(rows.sort().join('\n')),t.sha256);}else c.assert.equal(after.tables.find(n=>n.name===t.name).sha256,t.sha256,t.name+' changed');}
console.log(JSON.stringify({status:'passed',tables:after.tables.length,oldDataUnchanged:true}));
