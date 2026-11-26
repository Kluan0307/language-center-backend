// Local-only configuration; never print or pass the private key in command arguments.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const backend=path.resolve(__dirname,'../..'),website=path.resolve(backend,'../../language-center-website');
const files=[path.join(backend,'.env.local'),path.join(website,'.env.local')];
const texts=files.map(f=>fs.existsSync(f)?fs.readFileSync(f,'utf8'):'');
const value=(text,key)=>text.match(new RegExp('^\\s*'+key+'=(.*)$','m'))?.[1].trim().replace(/^(['"])(.*)\1$/,'$2')||'';
const before=texts.map(t=>value(t,'WEBSITE_FORM_SECRET'));
const configured=before.map(v=>v.length>=32);
let api=value(texts[1],'WEBSITE_API_URL'),local=false;
try{const u=new URL(api);local=['localhost','127.0.0.1'].includes(u.hostname)&&u.pathname.endsWith('/api/v1/academic/public/catalog/')&&!u.username&&!u.password&&!u.search&&!u.hash;}catch{}
const report={backendSecretConfigured:configured[0],websiteSecretConfigured:configured[1],matching:configured.every(Boolean)&&before[0]===before[1],websiteApiLocal:local,changed:false};
if(process.argv.includes('--apply')){
 const key=configured[0]?before[0]:configured[1]?before[1]:crypto.randomBytes(48).toString('base64');
 function set(text,name,v){const line=name+'='+v,regex=new RegExp('^\\s*'+name+'=.*$','m');return regex.test(text)?text.replace(regex,line):text.trimEnd()+'\n'+line+'\n';}
 const next=texts.map(t=>set(t,'WEBSITE_FORM_SECRET',key));
 if(!local)next[1]=set(next[1],'WEBSITE_API_URL','http://localhost:8080/api/v1/academic/public/catalog/');
 for(let i=0;i<files.length;i++)if(next[i]!==texts[i]){fs.writeFileSync(files[i],next[i]);report.changed=true;}
 report.backendSecretConfigured=true;report.websiteSecretConfigured=true;report.matching=true;report.websiteApiLocal=true;
}
console.log(JSON.stringify(report));
