import Link from "@/components/loading-link";
import {getCatalog} from "@/lib/catalog";
import {CourseCard} from "./course-card";
import {CatalogNotice} from "./catalog-notice";
export async function CourseCatalog({program}:{program:string}){try{const data=await getCatalog("COURSE",{program,size:6});return <section className="contentSection"><div className="sectionHeading"><div><p className="eyebrow">LỰA CHỌN THEO TRÌNH ĐỘ</p><h2>Khóa học đang giới thiệu</h2></div><Link className="textLink" href={"/khoa-hoc?program="+program}>Xem tất cả khóa học →</Link></div><div className="courseGrid">{data.items.map(c=><CourseCard course={c} key={c.slug}/>)}</div>{!data.items.length&&<p className="notice">Liên hệ trung tâm để được tư vấn khóa học phù hợp.</p>}</section>;}catch{return <CatalogNotice unavailable/>;}}
