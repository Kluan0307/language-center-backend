const fs=require('node:fs'),path=require('node:path');
const root='D:/hoctap/KLTN/language-center/language-center-website';
const postcss=require(root+'/node_modules/postcss');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const write=(p,s)=>fs.writeFileSync(path.join(root,p),s);
const css=postcss.parse(read('src/app/globals.css'));
const owned=/\.(?:header|catalogHeader|headerInner|brand|brandMark|brandLogo|desktopNav|headerUtility|headerCta|menuButton|mobileNav|catalogMobileNav|catalogNav|navDropdown|navChevron|navDropdownPanel|shell)(?![\w-])/;
css.walkRules(rule=>{
 if(rule.selector===':root'){rule.remove();return;}
 rule.selector=rule.selectors.filter(s=>!owned.test(s)).join(', ');
 if(!rule.selector){rule.remove();return;}
 rule.walkDecls(d=>{
  if(d.prop==='font-family'&&/Georgia|Arial|Times/.test(d.value))d.value='var(--font-sans), "Segoe UI", sans-serif';
  if(d.prop==='font-style'&&d.value==='italic')d.value='normal';
  if(d.prop==='font-weight')d.value=({'550':'600','650':'600','750':'700','800':'700'})[d.value]||d.value;
  if(d.prop==='top'&&/\.detailAside|\.infoPanel/.test(rule.selector))d.value='calc(var(--header-height) + var(--space-6))';
  if(d.prop==='scroll-padding-top')d.value='calc(var(--header-height) + var(--space-6))';
  if(d.prop==='font-size'&&/input|select|textarea/.test(rule.selector))d.value='1rem';
  if(d.prop==='font-size'&&/label/.test(rule.selector))d.value='0.875rem';
  if(d.prop==='font-weight'&&/label/.test(rule.selector))d.value='500';
  if(d.prop==='font-size'&&/^\.button$|^\.textLink$/.test(rule.selector))d.value='0.9375rem';
  if(d.prop==='font-size'&&rule.selector==='.eyebrow')d.value='0.75rem';
  if(d.prop==='letter-spacing'&&rule.selector==='.eyebrow')d.value='0.06em';
  if(d.prop==='padding-top'||d.prop==='padding-bottom')if(rule.selector==='.section')d.value='var(--section-space)';
  if(d.prop==='font-size'&&rule.selectors.includes('h2'))d.value='var(--text-heading)';
  if(d.prop==='letter-spacing'&&rule.selectors.includes('h2'))d.value='-0.025em';
  if(d.prop==='font-size'&&rule.selector==='.programCard h3')d.value='1.375rem';
  if(d.prop==='font-size'&&rule.selector==='.programCard .eyebrow')d.value='0.75rem';
  if(d.prop==='font-size'&&rule.selector==='.programLink')d.value='0.875rem';
  if(d.prop==='font-size'&&rule.selector==='.journeyStrip p')d.value='0.875rem';
  if(d.prop==='font-size'&&rule.selector==='.journeyStrip strong')d.value='1rem';
  if(d.prop==='font-size'&&rule.selector==='.cta h2')d.value='var(--text-heading)';
 });
 if(/input|select|textarea/.test(rule.selector)&&!/:disabled|checkbox|focus|consent|websiteTrap/.test(rule.selector))rule.append({prop:'font-weight',value:'400'});
 if(/h1 em/.test(rule.selector))rule.append({prop:'font-style',value:'normal'});
});
css.walkAtRules(r=>{if(r.nodes&&!r.nodes.length)r.remove();});
css.walkComments(c=>{if(/D6:|Shared brand asset/.test(c.text))c.remove();});
write('src/app/globals.css',css.toString());
let layout=read('src/app/layout.tsx').replace('import "./globals.css";',`import localFont from "next/font/local";
import "./tokens.css";
import "./globals.css";
import "./header.css";
const sans = localFont({
  src: [
    { path: "./fonts/BeVietnamPro-Regular.ttf", weight: "400", style: "normal" },
    { path: "./fonts/BeVietnamPro-Medium.ttf", weight: "500", style: "normal" },
    { path: "./fonts/BeVietnamPro-SemiBold.ttf", weight: "600", style: "normal" },
    { path: "./fonts/BeVietnamPro-Bold.ttf", weight: "700", style: "normal" },
  ],
  variable: "--font-sans", display: "swap", fallback: ["Segoe UI", "Arial", "sans-serif"],
});`)
.replace('<html lang="vi">','<html lang="vi" className={sans.variable}>');
write('src/app/layout.tsx',layout);
let header=read('src/components/header.tsx');
header=header.replace('<span className="navChevron" aria-hidden="true">⌄</span>','<svg className="navChevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>');
header=header.replace('Tư vấn ngay <span aria-hidden="true">↗</span>','Tư vấn ngay').replace('Tư vấn ngay ↗','Tư vấn ngay');
header=header.replace('{open ? "Đóng ×" : "Menu ☰"}','{open ? "Đóng" : "Menu"}<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d={open ? "m6 6 12 12M6 18 18 6" : "M4 6h16M4 12h16M4 18h16"} /></svg>');
// Keep the measured header height shared with every sticky/anchor offset, including zoom.
header=header.replace('  function active(href: string) {',`  useEffect(() => {
    const header = root.current;
    if (!header) return;
    const update = () => document.documentElement.style.setProperty("--header-height", header.getBoundingClientRect().height + "px");
    const observer = new ResizeObserver(update);
    observer.observe(header); update();
    return () => { observer.disconnect(); document.documentElement.style.removeProperty("--header-height"); };
  }, []);
  function active(href: string) {`);
write('src/components/header.tsx',header);
let home=read('src/app/page.tsx');
home=home.replace(/import \{ Arrow \}[^\n]+\n/,'').replace(/ <Arrow(?: diagonal)? \/>/g,'');
home=home.replace(/          <h1>[\s\S]*?          <\/h1>/,`          <h1>Tiếng Anh vững vàng. <em>Tương lai rộng mở.</em></h1>`);
home=home.replace(/            <span className=\{styles.imageLabel\}>[\s\S]*?            <span className=\{styles.photoCredit\}>/,`            <span className={styles.photoCredit}>`);
home=home.replace(/          <span className=\{styles.sideNote\}>.*?<\/span>\r?\n/,'');
home=home.replace(/<h2>\s*Một mục tiêu\.[\s\S]*?<\/h2>/,'<h2>Tìm chương trình phù hợp với bạn</h2>');
home=home.replace(/<h2>\s*Chọn điểm khởi đầu\.[\s\S]*?<\/h2>/,'<h2>Chương trình cho từng mục tiêu</h2>');
write('src/app/page.tsx',home);
for(const p of ['src/components/cta.tsx','src/components/program-card.tsx']){
 let s=read(p).replace(/import \{ Arrow \}[^\n]+\n/,'').replace(/\s*<Arrow(?: diagonal)? \/>/g,'');
 if(p.includes('program-card'))s=s.replace('{program.number} / {program.code}','{program.code}').replace('? "↗"','? "TOEIC"');
 write(p,s);
}
// Separate build artifacts from the running local dev server for verification.
write('next.config.ts',read('next.config.ts').replace('output:"standalone",','output:"standalone",distDir:process.env.WEBSITE_BUILD_DIR || ".next",'));
for(const name of ['tokens.css','header.css','page.module.css'])write('src/app/'+name,fs.readFileSync(path.join(__dirname,'website-u123-'+name),'utf8'));
console.log('Updated U1-U3 website sources.');
