const c=require('./data-common.cjs');
const before=c.dockerSnapshot();
for(const [table,name,columns] of [['students','idx_student_branch_id','branch_id,id'],['tuition','idx_tuition_student_due','student_id,due_date,id']]){
 const found=Number(c.mysql("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='"+table+"' AND index_name='"+name+"'"));
 if(!found)c.mysql('CREATE INDEX '+c.identifier(name)+' ON '+c.identifier(table)+'('+columns+')');
}
c.assert.deepEqual(c.dockerSnapshot(),before,'Application rows changed during index migration');
console.log('Lookup indexes installed; all row fingerprints unchanged.');
