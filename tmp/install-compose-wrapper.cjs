const fs=require('node:fs'),path=require('node:path');
const workspace=path.resolve(__dirname,'../../..'),target=path.join(workspace,'compose.yaml');
const source='name: language-center\ninclude:\n  - path: ./language-center-backend/language-center-backend/compose.yaml\n    env_file: ./.env.docker.local\n';
if(fs.existsSync(target)&&fs.readFileSync(target,'utf8')!==source)throw Error('Existing root Compose differs; preserve and review it');
fs.writeFileSync(target,source);
const ignore=path.join(workspace,'.gitignore'),old=fs.existsSync(ignore)?fs.readFileSync(ignore,'utf8'):'';
if(!old.split(/\r?\n/).includes('.env.docker.local'))fs.appendFileSync(ignore,'\n# Private Docker runtime configuration\n.env.docker.local\n');
console.log('Installed language-center/compose.yaml wrapper.');
