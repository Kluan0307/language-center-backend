const fs=require('fs'),assert=require('assert/strict'),{query,schema,root}=require('./database.cjs');process.chdir(root);
const db='lc_settings_'+Date.now(),sql=fs.readFileSync('docs/migrations/V022__web_settings.sql','utf8');query(null,'CREATE DATABASE `'+db+'`;');
try{query(db,schema());query(db,sql);query(db,sql);assert.equal(query(db,'SELECT COUNT(*) FROM company_profile WHERE id=1'),'1');if(process.argv.includes('--apply'))query('language_center_db',sql);console.log('PASS: V022 replay; applied='+process.argv.includes('--apply'));}finally{query(null,'DROP DATABASE `'+db+'`;');}
