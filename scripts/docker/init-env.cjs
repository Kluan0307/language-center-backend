const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'../..'),workspace=path.resolve(root,'../..');
const file=path.join(workspace,'.env.docker.local');
if(fs.existsSync(file)){console.log('Existing .env.docker.local preserved.');process.exit(0);}
let text=fs.readFileSync(path.join(root,'.env.docker.example'),'utf8');
for(const [key,size] of [['DB_PASSWORD',24],['MYSQL_ROOT_PASSWORD',32],['JWT_SECRET',48],['WEBSITE_FORM_SECRET',32]])text=text.replace(new RegExp('^'+key+'=$','m'),key+'='+crypto.randomBytes(size).toString('hex'));
for(const key of ['DB_PASSWORD','MYSQL_ROOT_PASSWORD','JWT_SECRET','WEBSITE_FORM_SECRET'])if(!new RegExp('^'+key+'=.+$','m').test(text))throw Error('Missing '+key+' template');
fs.writeFileSync(file,text,{flag:'wx',mode:0o600});
console.log('Created language-center/.env.docker.local with separate random Docker secrets. Values were not printed.');
