import type {Metadata} from "next";
import Link from "@/components/loading-link";
import {catalogOptions,getCatalogItem,audienceOptions} from "@/lib/catalog";
import {ConsultationForm} from "@/components/consultation-form";
import {site} from "@/lib/site";
export const dynamic="force-dynamic";
export const metadata:Metadata={title:"Đăng ký tư vấn",description:"Tư vấn chương trình, trình độ, chi nhánh và lịch học tiếng Anh phù hợp.",robots:{index:false,follow:true}};
export default async function Consultation({searchParams}:{searchParams:Promise<Record<string,string|string[]|undefined>>}){
 const q=await searchParams;const value=(key:string)=>typeof q[key]==="string"?q[key] as string:"";
 const [programs,branches,c,cl]=await Promise.all([catalogOptions("PROGRAM"),catalogOptions("BRANCH"),value("khoa-hoc")?getCatalogItem("COURSE",value("khoa-hoc")):null,value("lop")?getCatalogItem("CLASS",value("lop")):null]);
 const requestedAudience=value("doi-tuong")?(await audienceOptions()).find(a=>a.code===value("doi-tuong")):undefined;
 const requestedProgram=value("chuong-trinh")||c?.programSlug||"",program=programs.some(p=>p.slug===requestedProgram)?requestedProgram:"";
 const requestedBranch=value("chi-nhanh")||cl?.branchSlug||c?.branchSlug||"",branch=branches.some(b=>b.slug===requestedBranch)?requestedBranch:"";
 const course=c&&c.programSlug===program&&(!c.branchSlug||c.branchSlug===branch)?c.slug:"";
 const schoolClass=cl&&cl.courseSlug===course&&cl.branchSlug===branch?cl.slug:"";
 const stale=Boolean(value("khoa-hoc")&&!course||value("lop")&&!schoolClass||value("chuong-trinh")&&!program||value("chi-nhanh")&&!branch);
 return <div className="shell"><section className="pageHero compactHero"><nav className="breadcrumbs"><Link href="/">Trang chủ</Link><span>/</span><span>Tư vấn</span></nav><p className="eyebrow">BẮT ĐẦU TỪ MỤC TIÊU CỦA BẠN</p><h1>Cùng tìm lộ trình<br/><em>học tiếng Anh phù hợp.</em></h1><p>Chia sẻ nhu cầu của bạn. Đội ngũ tư vấn sẽ giúp bạn lựa chọn chương trình, chi nhánh và lịch học.</p></section>{stale&&<p className="formHint" role="status">Một số lựa chọn trước đó đã thay đổi. Vui lòng chọn lại thông tin phù hợp trước khi gửi.</p>}<div className="consultationLayout"><ConsultationForm initialNote={requestedAudience?"Quan tâm khóa học phù hợp đối tượng: "+requestedAudience.name+".":""} programs={programs} branches={branches} initial={{program,branch,course,schoolClass}}/><aside className="consultationAside"><div className="infoPanel"><p className="eyebrow">SAU KHI ĐĂNG KÝ</p><h2>Bước tiếp theo<br/>thật đơn giản.</h2><ol className="stepsList"><li><strong>Trao đổi nhu cầu</strong><p>Tư vấn viên chi nhánh bạn chọn sẽ liên hệ theo thông tin đã cung cấp.</p></li><li><strong>Xác định trình độ</strong><p>Được hướng dẫn đánh giá đầu vào nếu chương trình yêu cầu.</p></li><li><strong>Chọn lớp phù hợp</strong><p>Xác nhận lịch học, học phí và điều kiện ghi danh trước khi quyết định.</p></li></ol>{site.hotline&&<a className="contactLink" href={"tel:"+site.hotline.replace(/[^+0-9]/g,"")}>Gọi {site.hotline}</a>}{site.email&&<a className="contactLink" href={"mailto:"+site.email}>{site.email}</a>}</div><p className="asideNote">Thông tin được dùng để tiếp nhận và tư vấn nhu cầu học tập. Chúng tôi không yêu cầu mật khẩu hoặc thông tin thẻ thanh toán trong biểu mẫu.</p></aside></div></div>;
}
