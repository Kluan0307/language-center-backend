const c=require('./data-common.cjs');
const before=c.dockerSnapshot();
c.mysql(c.fs.readFileSync(c.path.join(c.root,'docs/migrations/V029__cloudinary_assets.sql'),'utf8'));
const after=c.dockerSnapshot();
c.assert.deepEqual(after.tables.filter(t=>before.tables.some(b=>b.name===t.name)),before.tables,'Existing data changed during media migration');
c.assert.ok(after.tables.some(t=>t.name==='media_assets'));
console.log(JSON.stringify({status:'passed',tables:after.tables.length,existingDataUnchanged:true}));
