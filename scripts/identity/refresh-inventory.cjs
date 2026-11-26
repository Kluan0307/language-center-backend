const fs=require('node:fs'),path=require('node:path');
const {query,root}=require('./database.cjs');
const tables=query('language_center_db',"SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME").split(/\r?\n/);
const columns=JSON.parse(query('language_center_db',"SELECT JSON_ARRAYAGG(JSON_OBJECT('table',TABLE_NAME,'column',COLUMN_NAME,'type',COLUMN_TYPE,'nullable',IS_NULLABLE,'default',COLUMN_DEFAULT,'extra',EXTRA)) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()"));
const fks=JSON.parse(query('language_center_db',"SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('table',TABLE_NAME,'column',COLUMN_NAME,'parent',REFERENCED_TABLE_NAME,'parentColumn',REFERENCED_COLUMN_NAME)),JSON_ARRAY()) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL"));
const dir=path.join(root,'storage/data-refresh');fs.mkdirSync(dir,{recursive:true});
fs.writeFileSync(path.join(dir,'inventory.json'),JSON.stringify({tables:tables.map(name=>({name,count:Number(query('language_center_db','SELECT COUNT(*) FROM `'+name+'`'))})),columns,fks},null,2));
console.log(tables.map(name=>name+': '+query('language_center_db','SELECT COUNT(*) FROM `'+name+'`')).join('\n'));
