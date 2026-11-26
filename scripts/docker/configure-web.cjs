// Re-runnable DCK4 configuration; existing secrets and custom URLs are preserved.
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..'),workspace=path.resolve(root,'../..'),file=path.join(workspace,'.env.docker.local');
if(!fs.existsSync(file))throw Error('Run node scripts/docker/init-env.cjs first.');
let source=fs.readFileSync(file,'utf8');
const values=Object.fromEntries(source.split(/\r?\n/).filter(l=>/^[A-Z_]+=/.test(l)).map(l=>[l.slice(0,l.indexOf('=')),l.slice(l.indexOf('=')+1)]));
const defaults={ADMIN_BIND_ADDRESS:'127.0.0.1',ADMIN_HOST_PORT:'15173',WEBSITE_BIND_ADDRESS:'127.0.0.1',WEBSITE_HOST_PORT:'13000',WEBSITE_INDEXABLE:'false',NEXT_PUBLIC_CENTER_PHONE:'',NEXT_PUBLIC_CENTER_EMAIL:''};
for(const [key,value] of Object.entries(defaults))if(!Object.hasOwn(values,key)){values[key]=value;source+='\n'+key+'='+value;}
if(!Object.hasOwn(values,'WEBSITE_PUBLIC_URL')){values.WEBSITE_PUBLIC_URL='http://localhost:'+values.WEBSITE_HOST_PORT;source+='\nWEBSITE_PUBLIC_URL='+values.WEBSITE_PUBLIC_URL;}
const origins=[values.WEBSITE_PUBLIC_URL,...['localhost','127.0.0.1'].flatMap(host=>[values.ADMIN_HOST_PORT,values.WEBSITE_HOST_PORT].map(port=>'http://'+host+':'+port))];
const allowed=[...new Set([...(values.CORS_ALLOWED_ORIGINS||'').split(',').filter(Boolean),...origins])].join(',');
if(/^CORS_ALLOWED_ORIGINS=/m.test(source))source=source.replace(/^CORS_ALLOWED_ORIGINS=.*$/m,'CORS_ALLOWED_ORIGINS='+allowed);else source+='\nCORS_ALLOWED_ORIGINS='+allowed;
fs.writeFileSync(file,source.trimEnd()+'\n');
fs.writeFileSync(path.join(workspace,'compose.yaml'),'name: language-center\ninclude:\n  - path:\n      - ./language-center-backend/language-center-backend/compose.yaml\n      - ./language-center-backend/language-center-backend/compose.web.yaml\n    env_file: ./.env.docker.local\n');
console.log('Root Compose and web origins configured. Existing secrets and custom ports preserved.');
