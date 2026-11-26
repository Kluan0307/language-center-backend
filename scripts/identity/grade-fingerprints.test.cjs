const {test}=require('node:test'),assert=require('node:assert/strict');
const {decimal,draftFingerprint,hash}=require('./grade-fingerprints.cjs');
test('BigDecimal tokens retain Java scientific notation for negative scales',()=>{
 for(const [input,expected] of [['10.00','1E+1'],['990.00','9.9E+2'],['450.00','4.5E+2'],['6.50','6.5'],['0.00','0'],['1.00','1'],['100.00','1E+2'],['0.50','0.5']])assert.equal(decimal(input),expected);
});
test('Draft hash matches the canonical Java JSON byte sequence',()=>{
 const data={name:'Bài 1',type:'TEST',date:'2026-11-15',maxScore:10,weight:1,frozen:true,candidates:[[401,401,'ENROLLMENT_PERIOD']],grades:[[401,8,'Đạt',0]]};
 const canonical='["Bài 1","TEST","2026-11-15",1E+1,1,true,[[401,401,"ENROLLMENT_PERIOD"]],[[401,8,"Đạt",0]]]';
 assert.equal(draftFingerprint(data),hash(canonical));
 assert.notEqual(draftFingerprint(data),hash(['Bài 1','TEST','2026-11-15',10,1,true,data.candidates,data.grades]));
});
