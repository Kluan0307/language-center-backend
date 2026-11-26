const fs=require('node:fs'),path=require('node:path');
const root='D:/hoctap/KLTN/language-center/language-center-website';
const postcss=require(root+'/node_modules/postcss');
const file=path.join(root,'src/app/globals.css'),css=postcss.parse(fs.readFileSync(file,'utf8'));
const bodies=[];css.walkRules('body',r=>bodies.push(r));
for(const r of bodies.slice(1)){for(const d of r.nodes)bodies[0].append(d.clone());r.remove();}
css.walkRules(r=>{
 if(r.selector==='button, input, select, textarea'){r.walkDecls('font-weight',d=>d.remove());}
 if(r.selector==='.footerBrand'&&r.nodes.length===3)r.remove();
});
const select=postcss.rule({selector:'select'});
for(const [prop,value] of Object.entries({appearance:'none','background-image':`url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E")`,'background-repeat':'no-repeat','background-position':'right 14px center','padding-right':'40px'}))select.append({prop,value});
// Put shared select and placeholder rules after component controls so shorthand backgrounds cannot erase the chevron.
css.append(select);
css.append(postcss.parse('input::placeholder,textarea::placeholder{color:#64748b;font-weight:400;opacity:1}'));
fs.writeFileSync(file,css.toString());
const audience=path.join(root,'src/components/audience-browse.tsx');fs.writeFileSync(audience,fs.readFileSync(audience,'utf8').replace(/ <span aria-hidden>↗<\/span>/g,''));
const page=path.join(root,'src/app/page.module.css');fs.writeFileSync(page,fs.readFileSync(page,'utf8').replace('.approach .eyebrow','.approach :global(.eyebrow)'));
// Build-only type discovery is harmless but should not pollute normal developer TS config.
const ts=path.join(root,'tsconfig.json'),config=JSON.parse(fs.readFileSync(ts,'utf8'));config.include=config.include.filter(p=>!p.startsWith('.next-ui-check/'));fs.writeFileSync(ts,JSON.stringify(config,null,2)+'\n');
console.log('Polished shared controls and audience links.');
