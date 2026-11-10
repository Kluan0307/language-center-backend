const fs=require('fs'),path=require('path'),crypto=require('crypto'),{execFileSync}=require('child_process');
const root=path.resolve(__dirname,'..'),folder=path.join(root,'docs/thesis/diagrams'),jar=path.join(root,'storage/tools/plantuml-1.2025.10.jar');
const names=fs.readdirSync(folder).filter(n=>/^(activity|sequence)-.*\.puml$/.test(n));
if(names.length!==10||!fs.existsSync(jar))throw Error('Expected 10 dynamic diagrams and local PlantUML jar');
const files=names.map(n=>path.join(folder,n));
for(const mode of ['-checkonly','-tsvg'])execFileSync('java',['-Djava.awt.headless=true','-jar',jar,'-charset','UTF-8',mode,...files],{stdio:'inherit',windowsHide:true,cwd:root});
const hash=f=>crypto.createHash('sha256').update(fs.readFileSync(f)).digest('hex');
const manifestFile=path.join(folder,'render-manifest.json'),manifest=JSON.parse(fs.readFileSync(manifestFile,'utf8'));
for(const name of names){const svg=name.replace('.puml','.svg'),text=fs.readFileSync(path.join(folder,svg),'utf8');if(!text.includes('<svg')||/Syntax Error|An error has occurred/.test(text))throw Error('Invalid render '+name);const entry=manifest.diagrams.find(x=>x.source===name);if(!entry)throw Error('Manifest entry missing');entry.sourceSha256=hash(path.join(folder,name));entry.svgSha256=hash(path.join(folder,svg));}
fs.writeFileSync(manifestFile,JSON.stringify(manifest,null,2)+'\n');console.log('Validated and rendered 10 corrected dynamic diagrams; manifest updated.');
