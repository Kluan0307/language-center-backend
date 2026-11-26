const c=require('./data-common.cjs');const {fs,path,root,assert,mysql}=c;const before=c.dockerSnapshot();
const columns=new Set(mysql("SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads'").trim().split(/\r?\n/));
const indexes=new Set(mysql("SELECT DISTINCT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='leads'").trim().split(/\r?\n/));
const constraints=new Set(mysql("SELECT constraint_name FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='leads'").trim().split(/\r?\n/));
const sql=fs.readFileSync(path.join(root,'docs/migrations/V033__lead_intake.sql'),'utf8').replace(/^--.*$/gm,'');
for(const statement of sql.split(';').map(s=>s.trim()).filter(Boolean)){
 const col=statement.match(/^ALTER TABLE leads ADD COLUMN (\w+)/i);if(col&&columns.has(col[1]))continue;
 const key=statement.match(/^ALTER TABLE leads ADD KEY (\w+)/i);if(key&&indexes.has(key[1]))continue;
 const fk=statement.match(/^ALTER TABLE leads ADD CONSTRAINT (\w+)/i);if(fk&&constraints.has(fk[1]))continue;
 const drop=statement.match(/^ALTER TABLE leads DROP INDEX (\w+)/i);if(drop&&!indexes.has(drop[1]))continue;
 mysql(statement+';');
}
const after=c.dockerSnapshot();for(const t of before.tables){if(t.name==='leads'){const rows=mysql('SELECT JSON_ARRAY('+t.columns.map(c.identifier).join(',')+') FROM leads').trim().split(/\r?\n/).filter(Boolean);assert.equal(c.sha(rows.sort().join('\n')),t.sha256);}else assert.equal(after.tables.find(n=>n.name===t.name).sha256,t.sha256,t.name+' changed');}
console.log(JSON.stringify({status:'passed',tables:after.tables.length,oldDataUnchanged:true,automaticConversion:false}));
