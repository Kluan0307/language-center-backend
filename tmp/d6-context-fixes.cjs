const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root='D:/hoctap/KLTN/language-center/language-center-website';
function edit(file,before,after){const target=path.join(root,file),text=fs.readFileSync(target,'utf8');assert.equal(text.split(before).length,2,file+' anchor');fs.writeFileSync(target,text.replace(before,after));}
edit('src/components/header.tsx','<details className="navDropdown" key={menu.label}', '<details className="navDropdown" key={menu.label} onToggle={event => { const detail = event.currentTarget; if (detail.open) root.current?.querySelectorAll<HTMLDetailsElement>("details[open]").forEach(other => { if (other !== detail) other.open = false; }); }}');
edit('src/components/course-card.tsx','{course:c,audience}:{course:CatalogItem;audience?:string}', '{course:c,audience,branch}:{course:CatalogItem;audience?:string;branch?:string}');
edit('src/components/course-card.tsx','const href="/khoa-hoc/"+c.slug+(audience?"?audience="+encodeURIComponent(audience):"");', 'const context=new URLSearchParams({...audience?{audience}:{},...branch?{branch}:{}});const href="/khoa-hoc/"+c.slug+(context.size?"?"+context:"");');
edit('src/app/khoa-hoc/page.tsx','<CourseCard key={c.slug} course={c} audience={q.audience}/>','<CourseCard key={c.slug} course={c} audience={q.audience} branch={q.branch}/>');
edit('src/app/khoa-hoc/page.tsx',' const [result,programs,branches,audiences]=', ' const advice=new URLSearchParams();if(q.audience)advice.set("doi-tuong",q.audience);if(q.program)advice.set("chuong-trinh",q.program);if(q.branch)advice.set("chi-nhanh",q.branch);\n const [result,programs,branches,audiences]=');
edit('src/app/khoa-hoc/page.tsx','href={q.audience?"/tu-van?doi-tuong="+encodeURIComponent(q.audience):"/tu-van"}', 'href={"/tu-van"+(advice.size?"?"+advice:"")}');
edit('src/app/khoa-hoc/[slug]/page.tsx','getCatalog("CLASS",{course:c.slug,page,size:12})','getCatalog("CLASS",{course:c.slug,page,size:12,...q.branch?{branch:q.branch}:{}})');
edit('src/app/khoa-hoc/[slug]/page.tsx','if(c.branchSlug)consultation.set("chi-nhanh",c.branchSlug);','if(c.branchSlug||q.branch)consultation.set("chi-nhanh",c.branchSlug||q.branch!);');
console.log('Preserved branch/program consultation context and made dropdowns mutually exclusive.');
