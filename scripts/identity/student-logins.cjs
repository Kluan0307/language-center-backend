const {query}=require('./database.cjs');
if(process.argv.includes('--apply')){
 const {execFileSync}=require('node:child_process'),path=require('node:path'),assert=require('node:assert/strict');
 const rows=JSON.parse(query('language_center_db',`SELECT JSON_ARRAYAGG(JSON_OBJECT('id',s.id,'accountId',a.id,'username',a.username,'role',a.role)) FROM students s JOIN accounts a ON a.id=s.account_id WHERE s.id BETWEEN 1 AND 5`));
 assert.equal(rows.length,5);
 for(const r of rows){assert.equal(r.role,'STUDENT');assert.ok([`hv${String(r.id).padStart(4,'0')}`,`student${r.id}`].includes(r.username));}
 assert.equal(Number(query('language_center_db',`SELECT COUNT(*) FROM accounts WHERE username IN ('student1','student2','student3','student4','student5') AND id NOT IN (${rows.map(r=>r.accountId).join(',')})`)),0);
 const jar=path.join(process.env.USERPROFILE,'.m2/repository/org/springframework/security/spring-security-crypto/7.1.1/spring-security-crypto-7.1.1.jar');
 const hash=execFileSync('java',['--class-path',jar,'scripts/identity/SamplePassword.java'],{encoding:'utf8',windowsHide:true}).trim();
 assert.match(hash,/^\$2[aby]\$/);
 const sql=['START TRANSACTION;'];
 for(const r of rows){
  sql.push(`UPDATE accounts SET username='student${r.id}',password='${hash}',status=1,must_change_password=0,password_changed_at=NOW(),session_version=session_version+1 WHERE id=${r.accountId} AND role='STUDENT';`);
  sql.push(`UPDATE student_account_provisionings SET username='student${r.id}',version=version+1 WHERE account_id=${r.accountId} AND student_id=${r.id};`);
 }
 sql.push('COMMIT;');query('language_center_db',sql.join('\n'));
 console.log('Updated five existing student logins; profiles and business records preserved.');
}
console.log(query('language_center_db',`SELECT s.id,s.student_code,s.full_name,s.account_id,a.username,a.role FROM students s LEFT JOIN accounts a ON a.id=s.account_id ORDER BY s.id LIMIT 8`));
console.log(query('language_center_db',`SELECT id,username,role FROM accounts WHERE username REGEXP '^student[0-9]+$'`));
console.log(query('language_center_db','SHOW COLUMNS FROM student_account_provisionings'));
console.log(query('language_center_db','SHOW COLUMNS FROM accounts'));
