const fs=require('node:fs'),path=require('node:path');
const root='D:/hoctap/KLTN/language-center/language-center-website';
const read=p=>fs.readFileSync(path.join(root,p),'utf8'),write=(p,s)=>fs.writeFileSync(path.join(root,p),s);
write('src/components/program-card.tsx',`import Link from "@/components/loading-link";
import type { Program } from "@/lib/programs";
import { PublicImage } from "./public-image";

export function ProgramCard({ program }: { program: Program }) {
  return (
    <Link href={"/chuong-trinh/" + program.slug} className={"programCard " + program.tone}>
      {program.imageUrl && <PublicImage src={program.imageUrl} alt={program.name} />}
      <div>
        <p className="eyebrow">{program.tag}</p>
        <h3>{program.name}</h3>
        <p>{program.description}</p>
      </div>
      <span className="programLink">Khám phá chương trình</span>
    </Link>
  );
}
`);
// Fetch data inside try/catch, render outside it so React rendering errors reach the error boundary.
write('src/components/audience-browse.tsx',`import Link from "@/components/loading-link";
import { audienceOptions } from "@/lib/catalog";

export async function AudienceBrowse({ programs }: { programs: { slug: string; code: string; name: string }[] }) {
  let groups: Awaited<ReturnType<typeof audienceOptions>>;
  try { groups = await audienceOptions(); } catch { return null; }
  if (!groups.length) return null;
  return (
    <section className="shell audienceBrowse">
      <p className="eyebrow">CHỌN HƯỚNG HỌC PHÙ HỢP</p>
      <h2>Tiếng Anh cho từng chặng đường.</h2>
      <div className="audienceLinks">
        {groups.map(a => <Link key={a.code} href={"/khoa-hoc?audience=" + a.code}>{a.name}</Link>)}
        {programs.filter(p => ["IELTS", "TOEIC"].includes(p.code)).map(p => <Link key={p.slug} href={"/khoa-hoc?program=" + p.slug}>{p.name}</Link>)}
      </div>
      <p className="formHint">Có thể kết hợp đối tượng và chương trình khi tìm khóa học.</p>
    </section>
  );
}
`);
const postcss=require(root+'/node_modules/postcss'),css=postcss.parse(read('src/app/globals.css'));
css.walkRules(r=>{
 if(/\.programCardTop|\.programGlyph/.test(r.selector)){r.remove();return;}
 if(r.selector==='.programCard')r.walkDecls('min-height',d=>d.value='0');
});
write('src/app/globals.css',css.toString());
write('eslint.config.mjs',read('eslint.config.mjs').replace('".next/**",','".next/**",\n    ".next-ui-check/**",'));
if(!read('.gitignore').includes('/.next-ui-check/'))write('.gitignore',read('.gitignore')+'\n# Isolated UI verification build\n/.next-ui-check/\n');
console.log('Simplified program cards and isolated build tooling.');
