const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const website='D:/hoctap/KLTN/language-center/language-center-website';
const staged=path.join(__dirname,'d6-files');
for(const [from,to] of [['navigation-model.ts','src/lib/navigation-model.ts'],['navigation.ts','src/lib/navigation.ts'],['header.tsx','src/components/header.tsx']])fs.copyFileSync(path.join(staged,from),path.join(website,to));
function edit(file,before,after){const target=path.join(website,file),text=fs.readFileSync(target,'utf8');assert.equal(text.split(before).length,2,'Edit anchor '+file);fs.writeFileSync(target,text.replace(before,after));}
edit('src/app/layout.tsx','import type { Metadata } from "next";','import type { Metadata } from "next";\nimport { Suspense } from "react";\nimport { getSiteNavigation } from "@/lib/navigation";');
edit('src/app/layout.tsx','export default function RootLayout(', 'export default async function RootLayout(');
edit('src/app/layout.tsx','  return (\n','  const menus = await getSiteNavigation();\n  return (\n');
edit('src/app/layout.tsx','<Header />','<Suspense fallback={<div className="header" aria-label="Điều hướng đang tải" />}><Header menus={menus} /></Suspense>');
fs.appendFileSync(path.join(website,'src/app/globals.css'),'\n'+fs.readFileSync(path.join(staged,'navigation.css'),'utf8'));
edit('src/components/course-card.tsx','export function CourseCard({course:c}:{course:CatalogItem}){return','export function CourseCard({course:c,audience}:{course:CatalogItem;audience?:string}){const href="/khoa-hoc/"+c.slug+(audience?"?audience="+encodeURIComponent(audience):"");return');
const cardPath=path.join(website,'src/components/course-card.tsx');fs.writeFileSync(cardPath,fs.readFileSync(cardPath,'utf8').replaceAll('href={"/khoa-hoc/"+c.slug}','href={href}'));
edit('src/app/khoa-hoc/page.tsx','<CourseCard key={c.slug} course={c}/>','<CourseCard key={c.slug} course={c} audience={q.audience}/>');
edit('src/app/khoa-hoc/[slug]/page.tsx','consultation.set("khoa-hoc",c.slug);','consultation.set("khoa-hoc",c.slug);if(q.audience&&c.classification?.audiences.some(a=>a.code===q.audience))consultation.set("doi-tuong",q.audience);');
console.log('Installed D6 navigation and preserved audience context in consultation links.');
