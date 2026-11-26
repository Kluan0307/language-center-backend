const c=require('./data-common.cjs'),{fs,path,workspace,root,assert}=c;const web=path.join(workspace,'language-center-web-frontend'),base=path.join(web,'src/components/Layout/Sidebar');
fs.copyFileSync(path.join(root,'tmp/header-layout/Topbar.tsx'),path.join(base,'Topbar.tsx'));
fs.mkdirSync(path.join(web,'public/brand'),{recursive:true});fs.copyFileSync(path.join(workspace,'language-center-website/public/brand/logo.svg'),path.join(web,'public/brand/logo.svg'));
function edit(file,fn){const p=path.join(web,file);fs.writeFileSync(p,fn(fs.readFileSync(p,'utf8')));}
edit('src/components/Layout/Sidebar/index.tsx',s=>{
 s=s.replace("import { Button, Drawer }", "import { Drawer }").replace("import { Menu as MenuIcon } from 'lucide-react';\n",'').replace('NavLink, useNavigate','NavLink').replace('getNavigationGroups, getItemLabel, ROLE_LABELS,','getNavigationGroups, getItemLabel,').replace("import AVATAR_DEFAULT from '../../../assets/images/avatar-default.png';\n",'').replace("import BranchSwitcher from './BranchSwitcher';","import Topbar from './Topbar';").replace('  const navigate = useNavigate();\n','');
 const start=s.indexOf('        <S.Header>'),end=s.indexOf('        <S.Menu aria-label=',start);if(start>=0){assert.ok(end>start);s=s.slice(0,start)+`        <S.Brand as={NavLink} to="/dashboard" onClick={()=>setOpen(false)} aria-label="Language Center · Trang tổng quan"><img src="/brand/logo.svg" alt=""/><span>LANGUAGE CENTER<small>ENGLISH EDUCATION</small></span></S.Brand>\n`+s.slice(end);}
 s=s.replace(/    <S.MobileBar>[\s\S]*?<\/S.MobileBar>/,'    <Topbar open={open} onMenu={()=>setOpen(true)}/>');s=s.replace('aria-label="Ti?n ?ch"','aria-label="Tiện ích"');return s;
});
edit('src/components/Layout/Sidebar/BranchSwitcher.tsx',s=>s.replace("<div style={{padding:'0 16px 12px'}}>",'<div>').replace('<Typography.Text type="secondary">Chi nhánh đang làm việc</Typography.Text>','<Typography.Text type="secondary" style={{fontSize:11,display:\'block\'}}>Chi nhánh đang làm việc</Typography.Text>').replace('marginTop:6','marginTop:4'));
edit('src/components/Layout/Sidebar/Sidebar.styles.ts',s=>{
 if(!s.includes('export const Brand'))s+=`\nexport const Brand=styled.a\`display:flex;align-items:center;gap:10px;min-height:60px;padding:0 8px 16px;border-bottom:1px solid #e2e8f0;text-decoration:none;color:#0f172a;img{width:40px;height:40px;border-radius:10px;flex-shrink:0}span{font-size:14px;font-weight:750;letter-spacing:-.2px;line-height:1.4}small{display:block;font-size:10px;letter-spacing:1.3px;color:#64748b;font-weight:500;margin-top:2px}&:focus-visible{outline:2px solid #2563eb;outline-offset:2px;border-radius:8px}\`;\n`;
 // The menu has its own scroll; document scrolling never moves the fixed sidebar.
 s=s.replace('    padding: 20px 14px;','    padding: 12px 14px 20px;');return s;
});
edit('src/components/Layout/ManagementLayout/ManagementLayout.styles.ts',s=>s.replace('padding: 28px 32px 40px;','padding: calc(var(--lc-shell-header-height, 80px) + 28px) 32px 40px;').replace('padding: 80px 16px 24px;', 'padding: calc(var(--lc-shell-header-height, 80px) + 24px) 16px 24px;'));
edit('src/pages/Overview/Overview.styles.ts',s=>s.replace('padding: 32px;', 'padding: calc(var(--lc-shell-header-height, 80px) + 28px) 32px 32px;').replace('padding-top: 80px;','padding-top: calc(var(--lc-shell-header-height, 80px) + 24px);'));
console.log('Shared topbar, center logo and branch switcher layout installed.');
