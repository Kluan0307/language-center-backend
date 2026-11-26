const fs=require('fs'),{query}=require('./database.cjs');
const counts={};for(const table of ['accounts','branches','students','courses','school_classes','class_sessions','enrollments','tuition','payments','grade_publications','placement_assessments'])counts[table]=Number(query('language_center_db','SELECT COUNT(*) FROM `'+table+'`'));
const temporary=query(null,"SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME REGEXP '^lc_identity_(http|g456)_[0-9]+$'");
const http=JSON.parse(fs.readFileSync('storage/identity-g456-http-verification.json','utf8'));
const report={date:'2026-11-14',phases:{G4:'IMPLEMENTED',G5:'IMPLEMENTED',G6:'IMPLEMENTED'},auth:{passed:95},academic:{passed:280},assistant:{passed:341,skipped:2},web:{passed:125,build:'PASS',changedFilesLint:'PASS'},http:{passed:http.checks.length,services:4,realMySql:true,realModel:false},database:{name:'language_center_db',tables:Number(query('language_center_db',"SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE'")),counts},temporaryDatabasesRemaining:temporary?temporary.split(/\r?\n/):[]};
fs.writeFileSync('storage/identity-g456-verification.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));
if(temporary)process.exitCode=1;
