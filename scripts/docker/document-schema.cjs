// Physical schema documentation only; no application rows or environment values.
const c=require('./data-common.cjs'),fs=require('fs'),path=require('path');
const rows=sql=>c.mysql(sql).split(/\r?\n/).filter(Boolean).map(JSON.parse);
const tables=rows("SELECT JSON_OBJECT('table',table_name,'engine',engine) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' ORDER BY table_name");
const columns=rows("SELECT JSON_OBJECT('table',table_name,'column',column_name,'type',column_type,'nullable',is_nullable,'key',column_key) FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,ordinal_position");
const foreignKeys=rows("SELECT JSON_OBJECT('table',table_name,'column',column_name,'parent',referenced_table_name,'parentColumn',referenced_column_name) FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND referenced_table_name IS NOT NULL ORDER BY table_name,column_name");
const indexes=rows("SELECT JSON_OBJECT('table',table_name,'name',index_name,'column',column_name,'position',seq_in_index) FROM information_schema.statistics WHERE table_schema=DATABASE() ORDER BY table_name,index_name,seq_in_index");
const old=JSON.parse(fs.readFileSync(path.join(c.root,'docs/thesis/design/schema-current.metadata.json'),'utf8'));
const oldNames=new Set(old.tables.map(t=>t.table));
const result={observedAt:new Date().toISOString(),source:'Docker physical schema; no business data',tableCount:tables.length,tables,columns,foreignKeys,indexes,addedSinceThesisSnapshot:tables.filter(t=>!oldNames.has(t.table)).map(t=>t.table),scope:'Physical ERD; logical cross-service references require code review and are not inferred as FK constraints.'};
const folder=path.join(c.root,'docs/docker');fs.writeFileSync(path.join(folder,'opt6-schema-metadata.json'),JSON.stringify(result,null,2));
const diagram=['flowchart LR',...tables.map(t=>'    '+t.table+'["'+t.table+'"]'),...foreignKeys.map(f=>'    '+f.table+' -->|"'+f.column+' → '+f.parentColumn+'"| '+f.parent)].join('\n');
fs.writeFileSync(path.join(folder,'opt6-physical-erd.mmd'),diagram+'\n');
console.log(JSON.stringify({tables:tables.length,columns:columns.length,foreignKeys:foreignKeys.length,addedSinceThesisSnapshot:result.addedSinceThesisSnapshot}));
