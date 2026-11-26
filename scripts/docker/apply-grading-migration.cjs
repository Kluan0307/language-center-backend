const c=require('./data-common.cjs');
const before=c.dockerSnapshot();
c.mysql(c.fs.readFileSync(c.path.join(c.root,'docs/migrations/V030__grading_schemes.sql'),'utf8'));
const after=c.dockerSnapshot();
c.assert.deepEqual(after.tables.filter(t=>before.tables.some(b=>b.name===t.name)),before.tables,'Existing table fingerprints changed during grading migration');
c.assert.equal(after.tables.filter(t=>t.name.startsWith('grading_')).length,7);
console.log(JSON.stringify({status:'passed',tables:after.tables.length,existingTablesUnchanged:true}));
