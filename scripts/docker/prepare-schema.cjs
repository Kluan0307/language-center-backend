// Schema-only snapshot of the current local DB. No rows/password values are exported.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const {schema,root}=require('../identity/database.cjs');
const sql=schema().replace(/ AUTO_INCREMENT=\d+/g,'');
assert.ok(!/^INSERT\s|^REPLACE\s/m.test(sql));assert.ok(!/^CREATE DATABASE|^USE /m.test(sql));
const tables=[...sql.matchAll(/^CREATE TABLE `([^`]+)`/gm)].map(x=>x[1]);assert.ok(tables.length>=80,'Unexpected schema snapshot');
const initializedTables=[...new Set([...tables,'media_assets'])].sort();
const target=path.join(root,'deployment/docker/mysql/001-schema.sql');
fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,'-- Schema only; generated from local schema on 2026-11-24. No application rows.\n'+sql);
fs.mkdirSync(path.join(root,'docs/docker'),{recursive:true});fs.writeFileSync(path.join(root,'docs/docker/schema-manifest.json'),JSON.stringify({generatedAt:new Date().toISOString(),tables:initializedTables,tableCount:initializedTables.length,snapshotTableCount:tables.length,additionalInitScripts:['deployment/docker/mysql/003-media-assets.sql'],rowsExported:0,sha256:crypto.createHash('sha256').update(fs.readFileSync(target)).digest('hex')},null,2));
console.log(JSON.stringify({tableCount:initializedTables.length,rowsExported:0}));
