const fs=require('node:fs'),path=require('node:path');
const center=path.resolve(__dirname,'../../../..'),root=path.join(center,'language-center-website'),mobile=path.join(center,'language-center-student-app');
if(!fs.existsSync(path.join(root,'package.json')))throw Error('Website root missing');
const logo=fs.readFileSync(path.join(mobile,'assets/language-center.svg'));
fs.mkdirSync(path.join(root,'public/brand'),{recursive:true});
fs.writeFileSync(path.join(root,'public/brand/logo.svg'),logo);fs.writeFileSync(path.join(root,'src/app/icon.svg'),logo);
for(const [file,type] of [['header.tsx','header'],['footer.tsx','footer']]){
 const dest=path.join(root,'src/components',file);let source=fs.readFileSync(dest,'utf8');
 if(!source.includes('import Image from'))source='import Image from "next/image";\n'+source;
 if(type==='header'){
  // Keep the client directive as the first statement.
  source=source.replace('import Image from "next/image";\n"use client";','"use client";\nimport Image from "next/image";');
  source=source.replace(/<span className="brandMark">\s*lc<span>\.<\/span>\s*<\/span>/,'<Image className="brandLogo" src="/brand/logo.svg" alt="" width={56} height={56} priority />');
 }else source=source.replace('Language Center<span>↗</span>','<Image className="brandLogo" src="/brand/logo.svg" alt="" width={44} height={44} />Language Center');
 fs.writeFileSync(dest,source);
}
const cssPath=path.join(root,'src/app/globals.css');let css=fs.readFileSync(cssPath,'utf8');
for(const [from,to]of Object.entries({'--cream: #faf9f5':'--cream: #f8fafc','--navy: #142b45':'--navy: #1f2937','--blue: #2459d9':'--blue: #2563eb','--lime: #def0ad':'--lime: #dbeafe','--muted: #566576':'--muted: #64748b','--border: #e2e5e7':'--border: #e2e8f0','background: #1747be':'background: #1d4ed8','background: #d1e69b':'background: #bfdbfe','background: #e5eddf':'background: #eff6ff','background: #e5edf8':'background: #dbeafe','background: #f6e6db':'background: #eef2ff','background: #ece9f1':'background: #f1f5f9'}))css=css.replaceAll(from,to);
if(!css.includes('/* Shared brand asset from the student app. */'))css+='\n/* Shared brand asset from the student app. */\n.brandLogo{display:block;flex-shrink:0;border-radius:12px}.footerBrand{align-items:center;justify-content:flex-start;gap:12px}@media(max-width:480px){.brand>.brandLogo{width:48px;height:48px}}\n';
fs.writeFileSync(cssPath,css);
if(!fs.readFileSync(path.join(root,'README.md'),'utf8').includes('## Nhận diện chung'))fs.appendFileSync(path.join(root,'README.md'),'\n## Nhận diện chung\n\nLogo sách mở được dùng trực tiếp từ `language-center-student-app/assets/language-center.svg`, hiển thị ở header/footer và favicon. Màu chính #2563EB, nền #F8FAFC, chữ #1F2937, viền #E2E8F0 đồng bộ app và web quản lý. Asset website: `public/brand/logo.svg`.\n');
console.log('Synchronized student-app logo and shared blue palette.');
