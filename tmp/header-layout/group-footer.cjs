const c=require('../../scripts/docker/data-common.cjs'),{fs,path,workspace,assert}=c;const p=path.join(workspace,'language-center-web-frontend/src/components/Layout/Sidebar/index.tsx');let s=fs.readFileSync(p,'utf8');s=s.replace("import { Drawer }", "import { Drawer,Dropdown }").replace("import { NavLink }", "import { NavLink,useLocation }").replace('LayoutDashboard, LogOut, Building2,','Menu as MenuIcon, LayoutDashboard, LogOut, Building2,');
const start=s.indexOf('      <S.SidebarBottom>'),end=s.indexOf('      </S.SidebarBottom>',start);assert.ok(start>=0&&end>start);s=s.slice(0,start)+'      <S.SidebarBottom><FooterUtilities onNavigate={()=>setOpen(false)}/></S.SidebarBottom>'+s.slice(end+'      </S.SidebarBottom>'.length);
s=s.replace('  const { user, logout } = useAuth();','  const { user } = useAuth();');
const component=`function FooterUtilities({onNavigate}:{onNavigate:()=>void}){
 const {user,logout}=useAuth();const location=useLocation();const [expanded,setExpanded]=useState(false);
 const manager=user?.role==='SUPER_ADMIN'||user?.role==='ADMIN';
 const items=[...(manager?[{key:'/website-content',label:<NavLink to="/website-content">Nội dung website</NavLink>,icon:<BookOpen size={17} aria-hidden="true"/>}]:[]),
 {key:'/company',label:<NavLink to="/company">Công ty</NavLink>,icon:<BuildingComplex size={17} aria-hidden="true"/>},
 {key:'/help',label:<NavLink to="/help">Hỗ trợ</NavLink>,icon:<CircleQuestionMark size={17} aria-hidden="true"/>},
 {key:'/settings',label:<NavLink to="/settings">Cài đặt</NavLink>,icon:<Settings size={17} aria-hidden="true"/>},
 {key:'divider',type:'divider' as const},
 {key:'logout',label:'Đăng xuất',icon:<LogOut size={17} aria-hidden="true"/>,danger:true}];
 return <Dropdown trigger={['click']} placement="topLeft" open={expanded} onOpenChange={setExpanded} menu={{items,selectedKeys:[location.pathname],onClick:({key})=>{setExpanded(false);onNavigate();if(key==='logout')logout();}}>
  <S.FooterItem type="button" aria-label="Mở menu tiện ích" aria-haspopup="menu" aria-expanded={expanded} className={items.some(item=>item.key===location.pathname)?'active':undefined}><MenuIcon size={20} aria-hidden="true"/><span>Thêm</span></S.FooterItem>
 </Dropdown>;
}

`;
s=s.replace('function Sidebar() {',component+'function Sidebar() {');fs.writeFileSync(p,s);console.log('Sidebar footer utilities grouped into hamburger dropdown.');
