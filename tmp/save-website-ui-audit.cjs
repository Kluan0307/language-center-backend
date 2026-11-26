const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..'),input=path.join(root,'tmp/website-visual-audit'),output=path.join(root,'docs/course-classification');
fs.copyFileSync(path.join(input,'audit.json'),path.join(output,'website-ui-audit.json'));
fs.copyFileSync(path.join(input,'font-audit.json'),path.join(output,'website-ui-font-audit.json'));
const photos=path.join(output,'website-ui-before');fs.mkdirSync(photos,{recursive:true});
for(const name of ['home-1440.png','home-390.png','home-320.png','khoa-hoc-1440.png','khoa-hoc-390.png','khoa-hoc-mg2-1440.png','tu-van-1440.png','tu-van-390.png'])fs.copyFileSync(path.join(input,name),path.join(photos,name));
console.log('Saved current visual/font evidence and before screenshots.');
