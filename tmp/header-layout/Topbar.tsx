import {useLayoutEffect,useRef} from 'react';
import {Button,Dropdown,Tag} from 'antd';
import {ChevronDown,Menu as MenuIcon,UserRound,KeyRound,LogOut,Building2} from 'lucide-react';
import {useNavigate} from 'react-router-dom';
import styled from 'styled-components';
import {useAuth} from '../../../hooks/useAuth';
import {ROLE_LABELS} from '../../../config/navigation';
import AVATAR_DEFAULT from '../../../assets/images/avatar-default.png';
import BranchSwitcher from './BranchSwitcher';
const Bar=styled.header`
 position:fixed;inset:0 0 auto 260px;z-index:800;display:flex;align-items:center;justify-content:space-between;gap:24px;min-height:80px;padding:12px 28px;background:#fff;border-bottom:1px solid #e2e8f0;box-sizing:border-box;
 .header-menu{display:none}.header-tools{display:flex;align-items:center;justify-content:space-between;gap:24px;min-width:0;flex:1}
 .header-branch{width:230px;min-width:0}.header-account{border:0;border-left:1px solid #e2e8f0;padding:0 0 0 24px;background:transparent;color:#0f172a;display:flex;align-items:center;gap:10px;min-width:0;max-width:300px;text-align:left;cursor:pointer;font:inherit}
 .header-account:focus-visible{outline:2px solid #2563eb;outline-offset:4px;border-radius:6px}.header-account:hover .header-name{color:#2563eb}
 .header-avatar{width:40px;height:40px;object-fit:cover;border-radius:50%;border:1px solid #e2e8f0;flex-shrink:0;background:#f1f5f9}.header-copy{display:grid;gap:3px;min-width:0}.header-name{font-size:14px;font-weight:650;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:210px}.header-position{font-size:12px;color:#64748b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:210px}
 .header-system{display:flex;align-items:center;gap:8px;justify-content:flex-start;color:#64748b;font-size:13px}.header-system .ant-tag{margin:0}
 @media(max-width:1150px){.header-tools{width:100%;justify-content:space-between}.header-account{max-width:280px}}
 @media(max-width:900px){left:0;padding:12px 16px;gap:16px;.header-menu{display:inline-flex}.header-tools{width:auto;flex:1}.header-branch{width:210px}.header-account{padding-left:16px;max-width:270px}.header-name,.header-position{max-width:180px}}
 @media(max-width:600px){padding:10px 12px;display:grid;grid-template-columns:auto minmax(0,1fr);gap:10px 12px;.header-menu{grid-column:1;grid-row:1}.header-tools{display:contents}.header-account{grid-column:2;grid-row:1;justify-self:end;border:0;padding:0;max-width:100%}.header-avatar{height:34px;width:34px}.header-name{font-size:13px}.header-position{font-size:11px}.header-name,.header-position{max-width:calc(100vw - 172px)}.header-branch{grid-column:1/-1;grid-row:2;width:100%}.header-system{justify-content:flex-start;min-height:32px}}
`;
export default function Topbar({open,onMenu}:{open:boolean;onMenu:()=>void}){
 const {user,logout}=useAuth();const navigate=useNavigate();const ref=useRef<HTMLElement>(null);
 useLayoutEffect(()=>{const element=ref.current;if(!element)return;const sync=()=>document.documentElement.style.setProperty('--lc-shell-header-height',element.getBoundingClientRect().height+'px');sync();const observer=new ResizeObserver(sync);observer.observe(element);return()=>{observer.disconnect();document.documentElement.style.removeProperty('--lc-shell-header-height');};},[]);
 const name=user?.fullName||user?.username||'Người dùng';const position=user?.position?.trim()||(user?.role?ROLE_LABELS[user.role]:'Chưa xác thực');
 return <Bar ref={ref} aria-label="Thanh thông tin tài khoản và chi nhánh">
  <Button className="header-menu" aria-label="Mở menu điều hướng" aria-expanded={open} aria-controls="mobile-navigation" icon={<MenuIcon size={20}/>} onClick={onMenu}>Menu</Button>
  <div className="header-tools"><div className="header-branch">{user?.role==='SUPER_ADMIN'?<div className="header-system"><Building2 size={16} aria-hidden="true"/><span>Phạm vi</span><Tag>Toàn hệ thống</Tag></div>:<BranchSwitcher/>}</div>
   <Dropdown trigger={['click']} placement="bottomRight" menu={{items:[{key:'profile',label:'Hồ sơ của tôi',icon:<UserRound size={16}/>,onClick:()=>navigate('/profile')},{key:'password',label:'Đổi mật khẩu',icon:<KeyRound size={16}/>,onClick:()=>navigate('/change-password')},{type:'divider'},{key:'logout',label:'Đăng xuất',danger:true,icon:<LogOut size={16}/>,onClick:logout}]}}>
    <button type="button" className="header-account" aria-label={'Menu tài khoản '+name}><img className="header-avatar" src={AVATAR_DEFAULT} alt=""/><span className="header-copy"><span className="header-name" title={name}>{name}</span><span className="header-position" title={position}>{position}</span></span><ChevronDown size={16} aria-hidden="true"/></button>
   </Dropdown>
  </div>
 </Bar>;
}
