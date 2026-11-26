// Import credentials only on explicit invocation; enabled flags stay opt-in.
const fs=require('fs'),path=require('path'),c=require('./data-common.cjs');
const file=path.join(c.workspace,'.env.docker.local');
const parse=text=>Object.fromEntries(text.replace(/^\uFEFF/,'').split(/\r?\n/).flatMap(l=>{const m=l.match(/^\s*([A-Z_]+)=(.*)$/);return m?[[m[1],m[2].trim().replace(/^(['"])(.*)\1$/,'$2')]]:[];}));
let text=fs.readFileSync(file,'utf8');const target=parse(text);
if(process.argv.includes('--import-local')){
 const source=parse(fs.readFileSync(path.join(c.root,'.env.local'),'utf8'));
 for(const key of ['VNPAY_TMN_CODE','VNPAY_HASH_SECRET','GEMINI_API_KEY','GEMINI_MODEL','EXPO_PROJECT_ID','EXPO_PUSH_ACCESS_TOKEN'])if(!target[key]&&source[key]){target[key]=source[key];if(new RegExp('^'+key+'=.*$','m').test(text))text=text.replace(new RegExp('^'+key+'=.*$','m'),()=>key+'='+source[key]);else text+='\n'+key+'='+source[key];}
 fs.writeFileSync(file,text);
}
const missing=keys=>keys.filter(k=>!target[k]);
const result={checkedAt:new Date().toISOString(),credentialsPrinted:false,providers:{vnpay:{enabled:target.VNPAY_ENABLED==='true',missing:missing(['VNPAY_TMN_CODE','VNPAY_HASH_SECRET','VNPAY_RETURN_URL']),callbackNeedsDockerGateway:true},gemini:{enabled:target.ASSISTANT_CHAT_ENABLED==='true',missing:missing(['GEMINI_API_KEY','GEMINI_MODEL'])},push:{enabled:target.NOTIFICATIONS_PUSH_ENABLED==='true',missing:missing(['EXPO_PROJECT_ID']),physicalDeviceRequired:true}}};
fs.writeFileSync(path.join(c.root,'docs/docker/opt3-integrations.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result,null,2));
