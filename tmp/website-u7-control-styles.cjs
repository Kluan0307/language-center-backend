const fs=require('node:fs'),root='D:/hoctap/KLTN/language-center/language-center-website';
const postcss=require(root+'/node_modules/postcss'),file=root+'/src/app/pages.css',css=postcss.parse(fs.readFileSync(file,'utf8'));
const selector='.catalogFilters input, .catalogFilters select, .filterForm select, .branchSearch input, .consultationForm input:not([type=checkbox]), .consultationForm select, .consultationForm textarea';
css.walkRules(rule=>{
 if(rule.selector.includes('.catalogFilters input')&&rule.nodes.every(d=>d.prop==='scroll-margin-block'))rule.remove();
 else if(rule.selector==='.consultationForm textarea'&&rule.nodes.some(d=>d.prop==='width'))rule.selector=selector;
});
const focus=postcss.rule({selector:'.consultationForm :is(input, select, textarea)'});focus.append({prop:'scroll-margin-block',value:'calc(var(--header-height) + 16px) 24px'});
css.walkRules(rule=>{if(rule.selector==='.consultationForm textarea'&&rule.nodes.some(d=>d.prop==='resize'))rule.before(focus);});
fs.writeFileSync(file,css.toString());
