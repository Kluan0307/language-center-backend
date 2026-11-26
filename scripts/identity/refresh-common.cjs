const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const db=require('./database.cjs'),dir=path.join(db.root,'storage/data-refresh');fs.mkdirSync(dir,{recursive:true});
const literal=v=>v===null||v===undefined?'NULL':typeof v==='number'?String(v):"'"+String(v).replace(/\\/g,'\\\\').replace(/'/g,"''")+"'";
const sha=v=>crypto.createHash('sha256').update(typeof v==='string'?v:JSON.stringify(v)).digest('hex');
const read=(table,fields,target='language_center_db')=>JSON.parse(db.query(target,"SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT("+fields.map(f=>"'"+f+"',`"+f+'`').join(',')+")),JSON_ARRAY()) FROM `"+table+'`'));
const keep=['accounts','admins','staffs','teachers','super_admins','branches','account_branches','account_branch_events','employment_events','personnel_code_numbers','company_profile','organization_units','organization_assignments','job_titles','teacher_profile_events','internal_support_tickets','internal_support_replies','user_preferences','student_notification_preferences','programs','audience_groups'];
const extend=['program_levels','student_code_numbers'];
function snapshot(target){const inv=JSON.parse(fs.readFileSync(path.join(dir,'inventory.json')));return Object.fromEntries([...keep,...extend].map(t=>{const fields=inv.columns.filter(c=>c.table===t&&!['last_login','session_version'].includes(c.column)).map(c=>c.column).sort();return [t,sha(db.query(target,'SELECT '+fields.map(f=>'`'+f+'`').join(',')+' FROM `'+t+'` ORDER BY '+fields.map(f=>'`'+f+'`').join(',')))];}));}
function scratch(name){assert.match(name,/^lc_refresh_[a-z]+_\d+$/);return name;}
module.exports={...db,fs,path,assert,dir,literal,sha,read,keep,extend,snapshot,scratch};
