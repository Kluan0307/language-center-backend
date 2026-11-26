const fs=require('node:fs'),path=require('node:path');
const root='D:/hoctap/KLTN/language-center/language-center-website',backup=path.join(__dirname,'website-u456-before');fs.mkdirSync(backup,{recursive:true});
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const write=(p,s)=>{const target=path.join(root,p);if(fs.existsSync(target)){const saved=path.join(backup,p);if(!fs.existsSync(saved)){fs.mkdirSync(path.dirname(saved),{recursive:true});fs.copyFileSync(target,saved);}}fs.writeFileSync(target,s);};
const postcss=require(root+'/node_modules/postcss');
const owned=/\.(?:pageHero|compactHero|breadcrumbs|catalogFilters|filterForm|resultHeading|courseGrid|teacherGrid|courseCard|courseCardBody|teacherCard|teacherCardBody|branchCard|branchCardBody|cardKicker|courseClassification|courseFacts|teacherInitials|qualification|branchGrid|branchFacts|cardActions|branchSearch|emptyCatalog|noResults|notice|pagination|detailLayout|contentGrid|detailAside|infoPanel|enrollmentPanel|summaryFacts|contentSection|detailBlocks|topicList|classList|classCard|statusBadge|meetingList|preserveLines|mobileConsultBar|consultationLayout|consultationForm|formIntro|formGrid|fullWidth|optional|consent|submitButton|formHint|websiteTrap|formError|formSuccess|successIcon|stepsList|asideNote|consultationAside|inlineButton|srOnly|prose)(?![\w-])/;
const css=postcss.parse(read('src/app/globals.css'));
css.walkRules(r=>{r.selector=r.selectors.filter(s=>!owned.test(s)).join(', ');if(!r.selector){r.remove();return;}r.walkDecls(d=>{if(d.prop==='font-size'&&/\.footer/.test(r.selector)&&parseFloat(d.value)<12)d.value='12px';});});
css.walkAtRules(r=>{if(r.nodes&&!r.nodes.length)r.remove();});
write('src/app/globals.css',css.toString());
write('src/app/pages.css',fs.readFileSync(path.join(__dirname,'website-u456-pages.css'),'utf8'));
write('src/app/layout.tsx',read('src/app/layout.tsx').replace('import "./header.css";','import "./header.css";\nimport "./pages.css";'));
for(const component of ['consultation-form','advanced-filters'])write('src/components/'+component+'.tsx',fs.readFileSync(path.join(__dirname,'website-u456-'+component+'.tsx'),'utf8'));
const coursePage='src/app/khoa-hoc/page.tsx';let courses=read(coursePage);
courses=courses.replace('export const dynamic=', 'import {AdvancedFilters} from "@/components/advanced-filters";\nexport const dynamic=');
courses=courses.replace('<label>Đối tượng phù hợp','<AdvancedFilters active={Boolean(q.audience||q.branch||q.maxTuition)}><label>Đối tượng phù hợp');
courses=courses.replace('</select></label><button className="button" type="submit">Tìm khóa học','</select></label></AdvancedFilters><button className="button" type="submit">Tìm khóa học');
courses=courses.replace('<span aria-hidden>↗</span>','');
write(coursePage,courses);
// Preserve branch and audience when paging through recruiting classes.
const detail='src/app/khoa-hoc/[slug]/page.tsx';let course=read(detail);
course=course.replace(' const days:Record',` function classPage(target:number){const p=new URLSearchParams();for(const key of ["branch","audience"])if(q[key])p.set(key,q[key]!);p.set("page",String(target));return "?"+p+"#lop-tuyen-sinh";}
 const days:Record`);
course=course.replace('"?page="+(page-1)+"#lop-tuyen-sinh"','classPage(page-1)').replace('"?page="+(page+1)+"#lop-tuyen-sinh"','classPage(page+1)');
write(detail,course);
for(const item of ['src/app/chi-nhanh/[slug]/page.tsx','src/app/giao-vien/[slug]/page.tsx'])write(item,read(item).replace('<aside>','<aside className="detailAside">'));
// Render JSX outside the I/O error boundary.
let catalog=read('src/components/course-catalog.tsx');
catalog=catalog.replace('try{const data=await getCatalog("COURSE",{program,size:6});return','let data:Awaited<ReturnType<typeof getCatalog>>;try{data=await getCatalog("COURSE",{program,size:6});}catch{return <CatalogNotice unavailable/>;}return').replace('; }catch', '; }catch');
catalog=catalog.replace('; }catch{return <CatalogNotice unavailable/>;}', ';}').replace(';}catch{return <CatalogNotice unavailable/>;}}',';}');
write('src/components/course-catalog.tsx',catalog);
// Editorial arrows are removed from actions; labels and destinations remain intact.
function walk(folder){return fs.readdirSync(path.join(root,folder),{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(folder+'/'+e.name):[folder+'/'+e.name]);}
for(const p of [...walk('src/app'),...walk('src/components')].filter(p=>p.endsWith('.tsx')&&!p.includes('/api/'))){
 let source=read(p),next=source.replace(/import \{ Arrow \}[^\n]+\n/g,'').replace(/\s*<Arrow(?: diagonal)? \/>/g,'').replace(/[←→↗↓↑]/g,'');
 next=next.replace(/<h1>([\s\S]*?)<\/h1>/g,(_,content)=>'<h1>'+content.replace(/<br\s*\/?\s*>/g,'{" "}')+'</h1>');
 if(next!==source)write(p,next);
}
write('src/app/loading.tsx',`export default function Loading() {
  return <section className="shell pageLoading" role="status" aria-live="polite" aria-busy="true"><p>Đang tải thông tin…</p><div aria-hidden="true"><div className="loadingLine" /><div className="loadingLine short" /><div className="loadingLine" /></div></section>;
}
`);
console.log('Applied U4-U6 source changes. Originals retained in tmp/website-u456-before.');
