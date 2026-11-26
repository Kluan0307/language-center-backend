const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../..'),target=path.resolve(root,'../../.env.docker.local');
const source=fs.readFileSync(path.join(root,'.env.local'),'utf8');let text=fs.readFileSync(target,'utf8');
for(const key of ['CLOUDINARY_CLOUD_NAME','CLOUDINARY_API_KEY','CLOUDINARY_API_SECRET','CLOUDINARY_FOLDER']){
 const value=source.match(new RegExp('^'+key+'=(.*)$','m'))?.[1].trim();if(!value)throw Error('Missing '+key+' in backend .env.local');
 if(!new RegExp('^'+key+'=.+$','m').test(text)){const line=key+'='+value;if(new RegExp('^'+key+'=.*$','m').test(text))text=text.replace(new RegExp('^'+key+'=.*$','m'),()=>line);else text+='\n'+line;}
}
fs.writeFileSync(target,text.trimEnd()+'\n');console.log('Cloudinary Docker configuration prepared; existing values preserved, credentials not printed.');
